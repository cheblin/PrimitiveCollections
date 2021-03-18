# Java Primitive Collections

`This is nightly bleeding-edge release`

Yet another Java primitive's collections with support  of unsigned and nullable values features 

Took the best ideas from 

[Trove](https://bitbucket.org/trove4j/trove)

[HPPC](https://labs.carrotsearch.com/hppc.html)

[HPPC-RT](https://github.com/vsonnier/hppcrt)

[fastutil](https://fastutil.di.unimi.it/)

[Koloboke](https://github.com/leventov/Koloboke)

Added new features, unsigned and nullable primitive types, will be needed 
in the upcoming AdHoc protocol parser code generator.

First, I was interested in performance and memory efficiency. I have not to tried to achieve any compatibility with the existing Java collections API. In the primitive's realm, it is impossible. I use a new iterator's protocol that does not generate any garbage in the heap. For communication between caller and responder and for holding iterator state, the tag is used. The tag is allocated in the stack and has int or long the primitive type.