# @Skip and @Limit 

!!! summary ""
    Command method param extension

Annotates [pagination](https://orientdb.com/docs/last/Pagination.html) parameters (of course, may be used separately). 

```java
@Query("select from Model")
List<Model> getAll(@Skip int skip, @Limit int limit);
```

Parameter type may be any `Number` type (`Integer, `Long` etc)

See bundled [Pagination mixin](../../mixin/pagination.md) as usage example.
