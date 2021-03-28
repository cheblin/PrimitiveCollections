# Java Primitive Collections

`This is nightly bleeding-edge release`

Yet another Java primitive's collections with support  of unsigned and nullable types 

This project get the best from...

[Trove](https://bitbucket.org/trove4j/trove)  
[HPPC](https://labs.carrotsearch.com/hppc.html)  
[HPPC-RT](https://github.com/vsonnier/hppcrt)  
[fastutil](https://fastutil.di.unimi.it/)  
[Koloboke](https://github.com/leventov/Koloboke)

...add new features, unsigned and nullable primitive types.

First and foremost, I was interested in performance and memory efficiency.
The compatibility with the existing Java Collections API was the last thing I was interested in. 
Moreover, this is impossible in the  primitive's realm.

Each collection type has two parts,
**R**- read-only and **RW** - read-write. **RW** extends **R** so you can always easily get 
and pass read-only(**R**) collection interface from **RW**

The project used a special iterator "protocol" that does not generate any garbage in heap.
The process of the communication between the caller and the responder can be imagined as passing a baton in the relay 
where the `tag` serves as the baton. The `tag` is allocated in the stack and has `int` or `long` primitive types.

For example, iterate over and print the content of the `IntIntNullMap`  
The Map where:  
K - int  
V - nullable int
```java
public StringBuilder toString( StringBuilder dst ) {
    if (dst == null) dst = new StringBuilder( assigned * 10 );
    else dst.ensureCapacity( dst.length() + assigned * 10 );
    
    Producer src = producer();
    for (int tag = src.tag(); src.ok( tag ); dst.append( '\n' ), tag = src.tag( tag ))
    {
        dst.append( src.key( tag ) ).append( " -> " );
        
        if (src.hasValue( tag )) dst.append( src.value( tag ) );
        else dst.append( "null" );
    }
    return dst;
}
```
While to avoid cluttering garbage collector while iterating, [HPPC-RT](https://github.com/vsonnier/hppcrt) project used:  
>Pooled, recyclable iterators: ability to use iterators the usual way, without creating iterator instances dynamically at runtime. 
> That means in particular using the enhanced for loop without any dynamic allocation underneath.


### List of nullable primitives

This is an ordinary list of primitives with "nulls info" stored in BitSet which allocates a bit per item.
Working with a nullable list is straightforward.
```java
IntNullList.RW list = IntNullList.RW.of( 1, 2, null, 4, 5, null, null, null, 9 );

System.out.println( list.toString() );

list.add( 12    ) ;
list.add( null  ) ;
list.add( 122   ) ;
list.add( null  ) ;
list.add( 1222  ) ;
list.add( 12222 ) ;

System.out.println( "=======================" );
System.out.println( list );
```
printout
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

similar to nullable `IntIntNullMap` 
```java
IntIntNullMap.RW map = new IntIntNullMap.RW();
		
map.put( 0   , 10101010) ;// 0   -> 10101010
map.put( 1   , 11      ) ;// 1   -> 11
map.put( 2   , 22      ) ;// 2   -> 22
map.put( 3   , null    ) ;// 3   -> null
map.put( 4   , null    ) ;// 3   -> null
map.put( 5   , 55      ) ;// 5   -> 55
map.put( 8   , 88      ) ;// 8   -> 88
map.put( 9   , null    ) ;// 9   -> null
map.put( 10  , null    ) ;// 10  -> null

map.put( null, 0xFF    ) ;// null-> 255 !!!

tag = map.tag( 3 );
assert (map.contains( tag ));
assert (!map.hasValue( tag ));
if (map.hasValue( tag )) System.out.println( map.get( tag ) ); //skip null value

tag = map.tag( 1 );
assert (map.contains( tag ));
assert (map.hasValue( tag ));
if (map.hasValue( tag )) System.out.println( map.get( tag ) ); //print 11

tag = map.tag( 99 );
assert (!map.contains( tag ));//key 99 is not present
assert (!map.hasValue( tag ));


System.out.println( map );
```
printout
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

and
```java
ObjectIntNullMap.RW<String> oim = new ObjectIntNullMap.RW<>();

oim.put( null              , 777 ) ;
oim.put( "key -> null"     , null) ;
oim.put( "key -> value"    , 11  ) ;

tag = oim.tag( "Not exists") ;

assert (!oim.contains( tag ));
assert (!oim.hasValue( tag ));

tag = oim.tag( null );
assert (oim.contains( tag ));
assert (oim.hasValue( tag ));
System.out.println( oim.get( tag ) );

tag = oim.tag( "key -> null" );
assert (oim.contains( tag ));
assert (!oim.hasValue( tag ));

tag = oim.tag( "key -> value" );
assert (oim.contains( tag ));
assert (oim.hasValue( tag ));
System.out.println( oim.get( tag ) );

System.out.println( oim );
```
printout
```
777
11
null -> 777
key -> value -> 11
key -> null -> null
```

> ### Map's and Set's keys are nullable

The `BitsList` was created to store in tightly-packed form tiny-range values/enums that can be fitted in several (up to 7 bits). 
Like a list of "nullable-boolean" enum `BoolNull`
```java
@interface BoolNull {
    int
		    NULL  = 0,
		    FALSE = 1,
		    TRUE  = 2;
}


BitsList.RW bits = new BitsList.RW( 2 );//2 bits per item
bits.add( BoolNull.FALSE) ;
bits.add( BoolNull.TRUE ) ;
bits.add( BoolNull.TRUE ) ;
bits.add( BoolNull.NONE ) ;
bits.add( BoolNull.NONE ) ;
bits.add( BoolNull.NONE ) ;
bits.add( BoolNull.FALSE) ;
```
printout
```
0
1
1
2
2
2
0
```

Its value can be stored in two bits. Provided `BoolNullList` was built in this manner.  
Mentioned special form "enum" - [SlimEnum](https://github.com/cheblin/SlimEnum)  was used here.


All these collections are primitive-types-backed, no boxing/unboxing.

Bugs? --->> [issues](https://github.com/cheblin/PrimitiveCollections/issues)