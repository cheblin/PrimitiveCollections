# Java Primitive Collections

`This is nightly bleeding-edge release`

Yet another Java primitive's collections with support  of unsigned and nullable type 

Took the best ideas from 

[Trove](https://bitbucket.org/trove4j/trove)  
[HPPC](https://labs.carrotsearch.com/hppc.html)  
[HPPC-RT](https://github.com/vsonnier/hppcrt)  
[fastutil](https://fastutil.di.unimi.it/)  
[Koloboke](https://github.com/leventov/Koloboke)

Added new features, unsigned and nullable primitive types, will be needed 
in the upcoming AdHoc protocol parser code generator.

First and foremost, I was interested in performance and memory efficiency.
The compatibility with the existing Java Collections API was the last thing I was interested in. 
Moreover, this is impossible in the  primitive's realm.

Each collection type has two parts,
**R**- read-only and **RW** - read-write. **RW** extends **R** so you can always easily get 
and pass read-only(**R**) collection interface from **RW**

The project used a special iterator's "protocol" that does not generate any garbage in heap. 
For communication between caller and responder and for holding iterator state, 
the tag is used. The tag is allocated in the stack and has `int` or `long` primitive types.

For example, iterate over and print the content of the `IntIntNullMap`  
Map where:  
K - int  
V - nullable int
```java
public StringBuilder toString( StringBuilder dst ) {
    if (dst == null) dst = new StringBuilder( assigned * 10 );
    else dst.ensureCapacity( dst.length() + assigned * 10 );
    
    Producer src = producer();
    for (int tag = src.tag(); tag != -1; dst.append( '\n' ), tag = src.tag( tag ))
    {
        dst.append( src.key( tag ) ).append( " -> " );
        
        if (src.isNull( tag )) dst.append( "null" );
        else dst.append( src.value( tag ) );
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
IntNullList.RW list = new IntNullList.RW( 1, 2, null, 4, 5, null, null, null, 9 );
		
System.out.println( list.toString() );
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
```

similar to nullable `IntIntNullMap` 
```java
int t;

IntIntNullMap.RW map = new IntIntNullMap.RW();

int key = 1;
map.put( key, 11 ); // 1 -> 11
if ((t = map.tag( key )) != -1) System.out.println( map.get( t ) ); //print 11

map.put( 2, 22 ); // 2 -> 22

key = 3;
map.put( key ); // 3 -> null
if ((t = map.tag( key )) != -1) System.out.println( map.tag( t ) ); //skip null value

map.put( 4 ); // 3 -> null
map.put( 5, 55 );
map.put( 8, 88 );
map.put( 9 );// 3 -> null
map.put( 10 );// 3 -> null
System.out.println( map.toString() );
```
printout
```
11
2 -> 22
4 -> null
8 -> 88
9 -> null
10 -> null
1 -> 11
3 -> null
5 -> 55
```
The `BitsList` was created to store in tightly-packed form tiny-range values/enums that can be fitted in several (up to 7 bits). 
Like a list of "nullable-boolean" enum
```java
@interface NullableBoolean {
    int TRUE = 1, FALSE = 0, NONE = 2;
}
```
Its value can be stored in two bits. The special form "enum" - [SlimEnum](https://github.com/cheblin/SlimEnum)  was used here.


All these collections are primitive-types-backed