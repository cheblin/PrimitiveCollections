Here is the improved version of your instruction:

---

# AdHoc Java Primitive Collections

This project introduces a set of collection classes designed specifically for primitive data types in Java. The primary objective is to provide an efficient and convenient alternative to the standard Java generic collections, which only support object references. This project draws inspiration from several leading libraries for primitive collections, including:

- [Eclipse Collections](https://www.baeldung.com/java-eclipse-primitive-collections)
- [Primitive Collections for Java](https://pcj.sourceforge.net/)
- [Trove](https://bitbucket.org/trove4j/trove)
- [HPPC](https://labs.carrotsearch.com/hppc.html)
- [HPPC-RT](https://github.com/vsonnier/hppcrt)
- [fastutil](https://fastutil.di.unimi.it/)
- [Koloboke](https://github.com/leventov/Koloboke)

### Key Features

- **Support for All Primitive Types**: Includes both unsigned and nullable variants.
- **Memory-Optimized Collections**: Provides lists, sets, and maps optimized for each primitive type.
- **Polymorphic Algorithms**: Supports operations directly on primitive collections.
- **Dynamic Type Safety and Interoperability**: Ensures safe interaction with generic collections.
- **Customizable Strategies**: Allows custom hashing, equality, and ordering strategies for primitive elements.

This project aims to provide a comprehensive and consistent solution for working with primitive data types in Java, enhancing performance, reducing memory usage, and improving expressiveness.

### Collection Interfaces

Each collection type provides two interfaces: **R** (read-only) and **RW** (read-write). The **RW** interface extends **R**, enabling a seamless transition between read-only and read-write modes.

To maintain garbage collection efficiency during iteration, the [HPPC-RT](https://github.com/vsonnier/hppcrt) project, for instance, implements:
> Pooled, recyclable iterators: These iterators can be used in standard ways, including enhanced for loops, without dynamic instance creation at runtime, thus avoiding dynamic allocation.

This project employs a specialized iterator protocol based on the `long` primitive type `token`, completely eliminating heap garbage generation.

### Examples

#### `ObjectObjectMap` Example

```java
public String toString() { return toJSON(); }

@Override
public void toJSON(JsonWriter json) {
    if (0 < assigned) {
        json.preallocate(assigned * 10);
        int token = NonNullKeysIterator.INIT;
        
        if (K_is_string) {
            json.enterObject();
            if (hasNullKey) json.name().value(NullKeyValue);
            while ((token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT)
                json.name(NonNullKeysIterator.key(this, token).toString())
                    .value(NonNullKeysIterator.value(this, token));
            json.exitObject();
        } else {
            json.enterArray();
            if (hasNullKey) json.enterObject().name("Key").value().name("Value").value(NullKeyValue).exitObject();
            while ((token = NonNullKeysIterator.token(this, token)) != NonNullKeysIterator.INIT)
                json.enterObject()
                    .name("Key").value(NonNullKeysIterator.key(this, token))
                    .name("Value").value(NonNullKeysIterator.value(this, token))
                    .exitObject();
            json.exitArray();
        }
    } else {
        json.enterObject();
        if (hasNullKey) json.name().value(NullKeyValue);
        json.exitObject();
    }
}
```

#### Nullable Primitive List

The list stores "nulls info" using a `BitSet` that assigns one bit per item, making interaction straightforward and intuitive.

```java
IntNullList.RW list = new IntNullList.RW(new Integer[]{1, 2, null, 4, 5, null, null, null, 9});
System.out.println(list.toString());
list.add(12);
list.add(null);
list.add(122);
list.add(null);
list.add(1222);
list.add(12222);
System.out.println("=======================");
System.out.println(list);
```

**Output:**
```
1
2
null
4
5
null
null
null
9

=======================
1
2
null
4
5
null
null
null
9
12
null
122
null
1222
12222
```

#### Nullable Map Example

Similar to the nullable `IntIntNullMap`:

```java
IntIntNullMap.RW map = new IntIntNullMap.RW(8);
map.put(0, 10101010); // 0 -> 10101010
map.put(1, 11); // 1 -> 11
map.put(2, 22); // 2 -> 22
map.put(3, null); // 3 -> null
map.put(4, null); // 4 -> null
map.put(5, 55); // 5 -> 55
map.put(8, 88); // 8 -> 88
map.put(9, null); // 9 -> null
map.put(10, null); // 10 -> null
map.put(null, 0xFF); // null -> 255 !!!

// Example token usage
switch (token = map.token(3)) {
    case Positive_Values.NONE:
        assert map.hasNone(token);
        System.out.println("none");
        break;
    case Positive_Values.NULL:
        assert map.hasNull(token);
        System.out.println("null");
        break;
    default:
        assert map.hasValue(token);
        System.out.println(map.value(token));
}
```

**Output:**
```
11
null -> 255
0 -> 10101010
2 -> 22
4 -> null
8 -> 88
9 -> null
10 -> null
1 -> 11
3 -> null
5 -> 55
```

### Compact Storage with `BitsList`

The `BitsList` efficiently stores small-range values/enums within a compact bit range, typically up to 7 bits, such as a list of "nullable-boolean" enums represented by `BoolNull`.

```java
@interface BoolNull {
    int NULL = 0, FALSE = 1, TRUE = 2;
}

BitsList.RW bits = new BitsList.RW(3); // 3 bits per item
bits.add(BoolNull.FALSE);
bits.add(BoolNull.TRUE);
bits.add(BoolNull.TRUE);
bits.add(BoolNull.NULL);
bits.add(BoolNull.NULL);
bits.add(BoolNull.NULL);
bits.add(BoolNull.FALSE);

System.out.println(bits);
```

**Output:**
```javascript
[
    1,
    2,
    2,
    0,
    0,
    0,
    1
]
```


### Report Issues

Found a bug? Report it [here](https://github.com/cheblin/PrimitiveCollections/issues).

