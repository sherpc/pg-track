# pg-track

A Clojure library that allows create Postgresql DDL via DSL.
Also supports restoring DSL from database and creating diff migrations.

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

### Типы данных

### Опции

## License

Copyright © 2015 Aleksandr Sher

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
