# Java Primitive Collections
Yet another Java primitive's collections with support  of unsigned and nullable values features 

Took the best ideas from 

[Trove](https://bitbucket.org/trove4j/trove)

[HPPC](https://labs.carrotsearch.com/hppc.html)

[HPPC-RT](https://github.com/vsonnier/hppcrt)

[fastutil](https://fastutil.di.unimi.it/)

[Koloboke](https://github.com/leventov/Koloboke)

Added new features like unsigned and nullable primitive values that will be needed 
in the upcoming AdHoc protocol parser code generator.

First, I was interested in performance and memory efficiency. 
I have not to tried to achieve any compatibility with the existing Java 
collections API. In the primitive's realm, it is impossible. I use a new 
iterator's protocol that does not generate any garbage in the heap. To hold its state 
and for communication, the tag is used, tag - primitive value int or long.