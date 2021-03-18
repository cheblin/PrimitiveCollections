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

I use a special iterator's protocol that does not generate any garbage in the heap. 
For communication between caller and responder and for holding iterator state, 
the tag is used. The tag is allocated in the stack and has `int` or `long` primitive types.