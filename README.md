# Java Primitive Collections: Cutting-Edge Nightly Release

Introducing a bleeding-edge nightly release of Java primitive collections, now with support for unsigned and nullable types.

This project draws inspiration from top-tier libraries:
- [Trove](https://bitbucket.org/trove4j/trove)
- [HPPC](https://labs.carrotsearch.com/hppc.html)
- [HPPC-RT](https://github.com/vsonnier/hppcrt)
- [fastutil](https://fastutil.di.unimi.it/)
- [Koloboke](https://github.com/leventov/Koloboke)

It introduces new features, including support for unsigned and nullable primitive types.

Performance and memory efficiency were paramount considerations. Compatibility with the existing Java Collections API was a secondary concern, given the unique challenges of the primitive realm.

Each collection type offers two interfaces: **R** for read-only and **RW** for read-write. The **RW** extends **R**, facilitating seamless transition between read-only (**R**) and read-write (**RW**) collection interfaces.

In order to uphold efficiency for the garbage collector during iteration, the [HPPC-RT](https://github.com/vsonnier/hppcrt) project, for example, implements:
> Pooled, recyclable iterators: Use iterators conventionally, without dynamic instance creation at runtime. This includes enhanced for loops without underlying dynamic allocation.

This project leverages a specialized iterator "protocol" based on the `long` primitive type `token`, completely eradicating heap garbage generation.

Here are some examples of using these iterators. Take a look at the `toString()` methods of collections. 
For instance, consider the `ObjectObjectMap`.

```java
    public String toString() { return toJSON(); }
    
    @Override public void toJSON( JsonWriter json ) {
        
        if( 0 < assigned ) {
            json.preallocate( assigned * 10 );
            int token = NonNullKeysIterator.INIT;
            
            if( K_is_string ) {
                json.enterObject();
                
                if( hasNullKey ) json.name().value( NullKeyValue );
                
                while( ( token = NonNullKeysIterator.token( this, token ) ) != NonNullKeysIterator.INIT )
                    json.
                            name( NonNullKeysIterator.key( this, token ).toString() ).
                            value( NonNullKeysIterator.value( this, token ) );
                
                json.exitObject();
            } else {
                json.enterArray();
                
                if( hasNullKey )
                    json.
                            enterObject()
                            .name( "Key" ).value()
                            .name( "Value" ).value( NullKeyValue ).
                            exitObject();
                
                while( ( token = NonNullKeysIterator.token( this, token ) ) != NonNullKeysIterator.INIT )
                    json.
                            enterObject()
                            .name( "Key" ).value( NonNullKeysIterator.key( this, token ) )
                            .name( "Value" ).value( NonNullKeysIterator.value( this, token ) ).
                            exitObject();
                
                json.exitArray();
            }
        } else {
            json.enterObject();
            if( hasNullKey ) json.name().value( NullKeyValue );
            json.exitObject();
        }
    }
```

### List of Nullable Primitives

Presenting a standard list of primitives accompanied by "nulls info," stored within a BitSet that assigns one bit per item. 
Interacting with a nullable list is uncomplicated and intuitive.

```java
    IntNullList.RW list = new IntNullList.RW( 1, 2, null, 4, 5, null, null, null, 9);
		
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
    IntIntNullMap.RW map = new IntIntNullMap.RW( 8 );
    
    map.put( 0, 10101010 );// 0   -> 10101010
    map.put( 1, 11 );// 1   -> 11
    map.put( 2, 22 );// 2   -> 22
    map.put( 3, null );// 3   -> null
    map.put( 4, null );// 4   -> null
    map.put( 5, 55 );// 5   -> 55
    map.put( 8, 88 );// 8   -> 88
    map.put( 9, null );// 9   -> null
    map.put( 10, null );// 10  -> null
    
    map.put( null, 0xFF );// null-> 255 !!!
    
    
    switch( token = map.token( 3 ) ) {
        case Positive_Values.NONE:
            assert map.hasNone( token );
            System.out.println( "none" );
            break;
        case Positive_Values.NULL:
            assert map.hasNull( token );
            System.out.println( "null" );
            break;
        default:
            assert map.hasValue( token );
            System.out.println( map.value( token ) );
    }
    
    token = map.token( 1 );
    
    assert map.hasValue( token );
    if( map.hasValue( token ) ) System.out.println( map.value( token ) ); //print 11
    
    token = map.token( 99 );
    assert ( map.hasNone( token ) );//key 99 is not present
    assert ( !map.hasValue( token ) );
    
    
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
    ObjectIntNullMap.RW< String > oim = new ObjectIntNullMap.RW<>( String.class, 4 );
    
    oim.put( null, 777 );
    oim.put( "key -> null", null );
    oim.put( "key -> value", 11 );
    
    token = oim.token( "Not exists" );
    assert oim.hasNone( token );
    assert !oim.hasValue( token );
    
    token = oim.token( null );
    assert ( oim.hasValue( token ) );
    System.out.println( oim.value( token ) );
    
    token = oim.token( "key -> null" );
    assert ( oim.hasNull( token ) );
    assert ( !oim.hasValue( token ) );
    
    token = oim.token( "key -> value" );
    assert ( oim.hasValue( token ) );
    System.out.println( oim.value( token ) );
    
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

```java
    ObjectObjectMap.RW< String,  String[]  > Map_str_array_of_str = new ObjectObjectMap.RW<>(  String.class , String[].class , 4 );
    Map_str_array_of_str.put( "Key0", new String[]{ "item0", "item1", "item12" } );
    Map_str_array_of_str.put( "Key1", new String[]{ "item0", "item1", "item12" } );
    Map_str_array_of_str.put( "Key2", new String[]{ "item0", "item1", "item12" } );
    System.out.println(Map_str_array_of_str);
```
printout
```javascript
[
	{
		"Key": "Key0",
		"Value": [
			"item0",
			"item1",
			"item12"
		]
	},
	{
		"Key": "Key1",
		"Value": [
			"item0",
			"item1",
			"item12"
		]
	},
	{
		"Key": "Key2",
		"Value": [
			"item0",
			"item1",
			"item12"
		]
	}
]

```

But if you need to construct a typed collection based on another typed collection, consider employing the following workaround:
```java
   ObjectObjectMap.RW< String, ObjectList.RW< String > > Map_str_List_of_str = new ObjectObjectMap.RW<>( String.class, ObjectList.of(), 4 );
    
    Map_str_List_of_str.put( "Key0", new ObjectList.RW<>( String.class, "item0", "item1", "item12" ) );
    Map_str_List_of_str.put( "Key1", new ObjectList.RW<>( String.class, "item0", "item1", "item12" ) );
    Map_str_List_of_str.put( "Key2", new ObjectList.RW<>( String.class, "item0", "item1", "item12" ) );
    
    System.out.println(Map_str_List_of_str);
```
printout
```javascript
[
	{
		"Key": "Key0",
		"Value": [
			"item0",
			"item1",
			"item12"
		]
	},
	{
		"Key": "Key1",
		"Value": [
			"item0",
			"item1",
			"item12"
		]
	},
	{
		"Key": "Key2",
		"Value": [
			"item0",
			"item1",
			"item12"
		]
	}
]
```

> ### Map's and Set's keys are nullable

The `BitsList` was developed to efficiently store small-range values/enums that can be accommodated within a tight bit range, 
typically up to 7 bits. An example use case is a list of "nullable-boolean" enums represented by `BoolNull`.

```java
@interface BoolNull {
    int
            NULL  = 0,
            FALSE = 1,
            TRUE  = 2;
}
	
	
    BitsList.RW bits = new BitsList.RW( 3 );//3 bits per item
    bits.add( BoolNull.FALSE );
    bits.add( BoolNull.TRUE );
    bits.add( BoolNull.TRUE );
    bits.add( BoolNull.NULL );
    bits.add( BoolNull.NULL );
    bits.add( BoolNull.NULL );
    bits.add( BoolNull.FALSE );
    
    
    System.out.println( bits );
```

printout

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

Its value can be compactly stored within two bits. This approach has been adopted for constructing the `BoolNullList`. Notably, a specialized form of 
"enum" called [SlimEnum](https://github.com/cheblin/SlimEnum) is utilized here.




These collections are all backed by primitive types, ensuring no need for boxing or unboxing operations.


Bugs? --->> [issues](https://github.com/cheblin/PrimitiveCollections/issues)