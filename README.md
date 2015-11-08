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

### Типы данных

## DSL

### Таблица

### Добавление колонки

### Общий dsl для описания таблицы

## License

Copyright © 2015 Aleksandr Sher

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
