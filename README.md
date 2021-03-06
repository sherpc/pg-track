# pg-track

A Clojure library that allows create Postgresql DDL via DSL.
Also supports restoring DSL from database and creating diff migrations.

Сейчас реализован DSL для хранения схемы и генерация CREATE TABLE sql кода по нему.

Потрачено времени (HH:mm):

 * Настройка emacs: 00:57
 * Активная разработка: 07:25
 * Размышления на ходу: 01:10
 * Убрать типы и новый разбор dsl: 01:20

**update 9.11**

Убрал типы в отдельный namespace. Реально сейчас типы никак не используются, перешел к варианту что тип -- это просто строка. Теперь можно добавлять любые типы, коих в постгрес великое множество :) Если в дальнейшем понадобится как-то работать с типами, то проще их разобрать из строки, чем писать отдельный dsl под работу с ними.

Из-за такого перехода немного усложнился разбор simple dsl, все-таки пришлось написать свой reduce вместо использования набора библиотечных функций (см. функцию [extract-columns](https://github.com/sherpc/pg-track/blob/8c25188cedcf1ff2b8d1d7b8ad6d527fa6274ced/src/pg_track/core.clj#L33) ).

Итого dsl немного поменялся, тип теперь выглядит не как :char 5, а просто "char(5)". README файл не правил, примеры старые.

**update 12.11**

Еще две мысли:

 * Внутреннее представление dsl должно быть удобно для генерации diff'а, т.к. эта задаче сложнее, чем вывести в sql. Поэтому сейчас надо быстро проверить, нет ли с первого взгляда подводных камней с восстановлением dsl по БД (написать простой парсер), и переходить к реализации diff. Там, скорее всего, и станет более понятно, как хранить ddl. Функции для конструирования dsl также всегда можно легко переписать.
 * Можно пойти совсем другим путем, взять какой-нибудь движок для парсеров и реализовать на нем грамматику psql. Вариант более универсальный, т.к. можно будет относительно просто покрыть полную спецификацию psql, и потом её можно будет легко менять. Периодически думаю про этот вариант, пока он все-таки кажется "из пушки по воробьям".

### Немножно грусти

Оказывается, гитхаб не поддерживает рендеринг --- в mdash :(
https://github.com/github/markup/issues/77

## Внутреннее представление

### Таблица

Таблица хранится как hashmap вида:

```clojure
{:name "table-name"
 :columns [vector-of-columns]
 :constraints [vector-of-constraints]}
```

Ключ `:constraints` пока не реализован.

### Колонка

Колонка это hashmap вида:

```clojure
{:name "column-name"
 :type column-type
 :options options-hashmap}
```

Про `column-type` смотри следующий раздел про типы данных.
Опции колонки --- это разнообразные constraints. Внутри хранится в виде hashmap, где ключ --- это идентификатор опции, а значение --- опциональное значение. Если значения нет, храним nil. Пример опций: `{:not-null nil :default 0}`.

### Типы данных

Для работы с типами данных сделан протокол TypeResolver:

```clojure
(defprotocol TypeResolver
  (get-sql-string [this type] "Get sql representation of type.")
  (is-type-valid? [this type] "Check that type is valid."))
```

Он позволяет получить sql-представление типа и проверить на существование и корректность.

Сейчас реализован SimpleTypeResolver. Он хранит множество допустимых типов в виде keyword'ов. При проверке мы просто проверяем, есть ли тип в множестве. Храним тип в виде вектора из двух элементов --- keyword типа и опционально размер. Например, `[:integer]`, или `[:char 5]`. В sql выводится как (name keyword) + размер, если нужно. Минусы текущей реализации --- нет проверки на размер типа (можно указать размер у integer и не указать размер у char, что некорректно для БД). В будущем, возможно подключить prismatic/scheme для хранения типов.

```clojure
(def column-types #{:char :varchar :integer :date})

(defn build-type-sql
  [[type size]]
  (str (clojure.core/name type)
       (when size (wrap-brackets size))))

(defrecord SimpleTypeResolver [types]
  TypeResolver
  (get-sql-string [_ type] (build-type-sql type))
  (is-type-valid? [_ [type]] (types type)))

(def resolver (->SimpleTypeResolver column-types))
```

## DSL

Есть "внутренний" dsl и более простой финальный (на текущий момент) вариант.

### Таблица

Все предельно просто --- функция от одного аргумента, название таблицы.

```clojure
(table* "table-name")

(def table-base (table* tbl-name))
(expect {:name tbl-name :columns []} table-base)
```

### Добавление колонки

Функция `column*` принимает таблицу, название колонки и keyword типа колонки. Дальше идет размер типа (если он нужен) и опции. Каждая опция --- либо просто keyword, либо вектор из двух элементов `[keyword value]`. Примеры:

```clojure
(def test-table-dsl
  (-> table-base
      (column* "code" :char 5)
      (column* "title" :varchar 40 :not-null [:check "title <> ''"])
      (column* "did" :integer :primary-key [:default "nextval('serial')"])
      (column* "date_prod" :date)))
```

### Общий dsl для описания таблицы

Объединяет два предыдущих. Функция `table` принимает название таблицы и последовательность аргументов для функции `column*`. Мы можем понять, как разбить эту последовательность на колонки, т.к. колонка всегда начинается с аргумента типа строка (название колонки), и строковых аргументов больше нет (это либо keyword типа или опции, либо number размера типа, либо вектор опции). Таблица из предыдущего примера может быть записана так:

```clojure
(def simple-dsl 
  (table tbl-name
         "code" :char 5
         "title" :varchar 40 :not-null [:check "title <> ''"]
         "did" :integer :primary-key [:default "nextval('serial')"]
         "date_prod" :date))
```

## SQL rendering

### Проверка на корректность схемы

Пока что просто для всех типов проверяется, что они корректны через TypeResolver.

```clojure
(defn table-is-valid?
  [{cs :columns}]
  (every? #(is-type-valid? resolver (:type %)) cs))

(expect true (table-is-valid? test-table))
;; Типа :bad-int не существует
(expect false (table-is-valid? (column* table-base "id" :bad-int)))
```

### Типы данных

SQL код генерируется через протокол TypeResolver.

### Опции

Есть hashmap с функциями для генерации кода опций:

```clojure
(def available-options
  {:primary-key (constantly "PRIMARY KEY")
   :not-null (constantly "NOT NULL")
   :default #(str "DEFAULT " %)
   :check #(str "CHECK " (wrap-brackets %))})
```

При генерации кода по keywrod'у ищется функция и вызывается с значением опции в качестве аргумента.

```clojure
(def opts {:primary-key nil :check "name <> ''" :not-null nil})

(expect 
 "PRIMARY KEY CHECK (name <> '') NOT NULL" 
 (options-sql opts))
```

Для расширения возможных обрабатываемых опций нужно просто расширить hashmap.

### Таблица

Склеиваем название таблицы, string/join колонок и внутри колонки string/join опций. Результат:

```clojure
(def sql (create-sql test-table-dsl))
(println sql)
```

```sql
CREATE TABLE films (
	code char(5),
	title varchar(40) NOT NULL CHECK (title <> ''),
	did integer PRIMARY KEY DEFAULT nextval('serial'),
	date_prod date
);
```

## Замечания

Для тестов использовал expectations. Сейчас код всего в двух файлах, core.clj и core_test.clj. Нужно разнести по разным файлам.

Также сейчас есть зависимость от clojure.jdbc и postgres драйвера, я пока не понял, насколько это хорошо. В идеале в финальной сборки зависимость должна быть только от jdbc (а драйвер постгреса каждый будет подключать вручную актуальной на момент подключения версии).

Никак не отрабатываются невалидные данные. Нужно дописать исключений, либо подумать над возвращаемым значением с ошибкой.

Убрать зависимость table-is-valid? от переменной resolver (вынести её в аргумент функции).

## License

Copyright © 2015 Aleksandr Sher

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
