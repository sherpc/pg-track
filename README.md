# pg-track

A Clojure library that allows create Postgresql DDL via DSL.
Also supports restoring DSL from database and creating diff migrations.

## Внутреннее представление

### Таблица

Таблица хранится как hashmap вида:

```clojure
{:name "table-name"
 :columns [vector-of-columns]
 :constraints [vector-of-constraints]}
```

### Колонка

Колонка это hashmap вида:

```clojure
{:name "column-name"
 :type column-type
 :options options-hashmap
}
```

Про `column-type` смотри следующий раздел про типы данных.
Опции колонки -- это разнообразные constraints. Внутри хранится в виде hashmap, где ключ -- это идентификатор опции, а значение -- опциональное значение. Если значения нет, храним nil. Пример опций: `{:not-null nil :default 0}`.

### Типы данных

Для работы с типами данных сделан протокол TypeResolver:

```clojure
(defprotocol TypeResolver
  (get-sql-string [this type] "Get sql representation of type.")
  (is-type-valid? [this type] "Check that type is valid."))
```

Он позволяет получить sql-представление типа и проверить на существование и корректность.

Сейчас реализован SimpleTypeResolver. Он хранит множество допустимых типов в виде keyword'ов. При проверке мы просто проверяем, есть ли тип в множестве. Храним тип в виде вектора из двух элементов -- keyword типа и опционально размер. Например, `[:integer]`, или `[:char 5]`. В sql выводится как (name keyword) + размер, если нужно. Минусы текущей реализации -- нет проверки на размер типа (можно указать размер у integer и не указать размер у char, что некорректно для БД). В будущем, возможно подключить prismatic/scheme для хранения типов.

## DSL

### Таблица

### Добавление колонки

### Общий dsl для описания таблицы

## SQL rendering

### Типы данных

### Опции

## License

Copyright © 2015 Aleksandr Sher

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
