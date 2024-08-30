//MIT License
//
//Copyright © 2020 Chikirev Sirguy, Unirail Group. All rights reserved.
//For inquiries, please contact:  al8v5C6HU4UtqE9@gmail.com
//GitHub Repository: https://github.com/AdHoc-Protocol
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to use,
//copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
//the Software, and to permit others to do so, under the following conditions:
//
//1. The above copyright notice and this permission notice must be included in all
//   copies or substantial portions of the Software.
//
//2. Users of the Software must provide a clear acknowledgment in their user
//   documentation or other materials that their solution includes or is based on
//   this Software. This acknowledgment should be prominent and easily visible,
//   and can be formatted as follows:
//   "This product includes software developed by Chikirev Sirguy and the Unirail Group
//   (https://github.com/AdHoc-Protocol)."
//
//3. If you modify the Software and distribute it, you must include a prominent notice
//   stating that you have changed the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT, OR OTHERWISE, ARISING FROM,
//OUT OF, OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

package org.unirail.collections;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

//Main interface for array operations
public interface Array
{
    //Inner class for equality and hash code computation, equivalent to C#'s IEqualityComparer
    class EqualHashOf<T>
    { //C#  IEqualityComparer (https://learn.microsoft.com/en-us/dotnet/api/system.collections.generic.iequalitycomparer-1?view=net-8.0) equivalent
        //Cached empty array instance of type T
        public final Object O;
        public final T[] OO;

        //Method to copy an array of type T
        @SuppressWarnings("unchecked")
        public T[] copyOf(T[] src, int len)
        {
            return len < 1 ? OO : Arrays.copyOf(src == null ? OO : src, len);
        }

        //Flag to indicate if the class is an array type
        private final boolean array;

        //Constructor to initialize the object with a specific class
        @SuppressWarnings("unchecked")
        public EqualHashOf(Class<T> clazz)
        {

            OO = (T[])java.lang.reflect.Array.newInstance(clazz, 0);

            O = (array = clazz.isArray()) ? java.lang.reflect.Array.newInstance(clazz.componentType(), 0) : OO;
        }

        //Method to compute hash code of an array
        @SuppressWarnings("unchecked")
        public int hashCode(T src)
        {
            return array ? hashCode((T[])src) : hash(src);
        }

        //Method to check equality between two objects
        public boolean equals(T v1, T v2) { return Objects.deepEquals(v1, v2); }

        //Method to compute hash code of an array
        public int hashCode(T[] src)
        {
            return src == null ? 0 : hashCode(src, src.length);
        }

        //Method to compute hash code of an array with specified size
        @SuppressWarnings("unchecked")
        public int hashCode(T[] src, int size)
        {
            int seed = EqualHashOf.class.hashCode();
            if (array)
            {
                while (-1 < --size)
                    seed = (size + 10153331) + hash(seed, hash(src[size]));
                return seed;
            }

            switch (size)
            {
                case 0:
                    return Array.finalizeHash(seed, 0);
                case 1:
                    return Array.finalizeHash(Array.mix(seed, Array.hash(src[0])), 1);
            }

            final int initial = Array.hash(src[0]);
            int prev = Array.hash(src[1]);
            final int rangeDiff = prev - initial;
            int h = Array.mix(seed, initial);

            for (int i = 2; i < size; ++i)
            {
                h = Array.mix(h, prev);
                final int hash = Array.hash(src[i]);
                if (rangeDiff != hash - prev)
                {
                    for (h = Array.mix(h, hash), ++i; i < size; ++i)
                        h = Array.mix(h, Array.hash(src[i]));

                    return Array.finalizeHash(h, size);
                }
                prev = hash;
            }

            return Array.avalanche(Array.mix(Array.mix(h, rangeDiff), prev));
        }

        //Method to check equality between two arrays with specified length
        @SuppressWarnings("unchecked")
        public boolean equals(T[] O, T[] X, int len)
        {
            if (O != X)
            {
                if (O == null || X == null || O.length < len || X.length < len)
                    return false;
                if (array)
                {
                    for (T o, x; -1 < --len;)
                        if ((o = O[len]) != (x = X[len]))
                            if (o == null || x == null || !Arrays.deepEquals((T[])o, (T[])x))
                                return false;
                }
                else
                    for (T o, x; -1 < --len;)
                        if ((o = O[len]) != (x = X[len]))
                            if (o == null || x == null || !equals(o, x))
                                return false;
            }
            return true;
        }

        //Method to check equality between two arrays
        public boolean equals(T[] O, T[] X) { return O == X || O != null && X != null && O.length == X.length && equals(O, X, O.length); }

        //Singleton instance for boolean arrays
        public static final _booleans booleans = new _booleans();

        //Inner class for equality and hash code computation on boolean arrays
        public static final class _booleans extends EqualHashOf<boolean[]>
        {
            public static final boolean[] O = new boolean[0];

            private _booleans() { super(boolean[].class); }

            @Override
            public int hashCode(boolean[] src) { return Arrays.hashCode(src); }

            @Override
            public boolean equals(boolean[] v1, boolean[] v2) { return Arrays.equals(v1, v2); }

            @Override
            public int hashCode(boolean[][] src, int size)
            {
                int hash = boolean[][].class.hashCode();
                while (-1 < --size)
                    hash = (size + 10153331) + hash(hash, Arrays.hashCode(src[size]));
                return hash;
            }

            @Override
            public boolean equals(boolean[][] O, boolean[][] X, int len)
            {
                if (O != X)
                {
                    if (O == null || X == null || O.length < len || X.length < len)
                        return false;
                    for (boolean[] o, x; -1 < --len;)
                        if ((o = O[len]) != (x = X[len]))
                            if (o == null || x == null || !Arrays.equals(o, x))
                                return false;
                }
                return true;
            }
        }

        //Singleton instance for byte arrays
        public static final _bytes bytes = new _bytes();

        //Inner class for equality and hash code computation on byte arrays
        public static final class _bytes extends EqualHashOf<byte[]>
        {
            public static final byte[] O = new byte[0];

            private _bytes() { super(byte[].class); }

            @Override
            public int hashCode(byte[] src) { return Arrays.hashCode(src); }

            @Override
            public boolean equals(byte[] v1, byte[] v2) { return Arrays.equals(v1, v2); }

            @Override
            public int hashCode(byte[][] src, int size)
            {
                int hash = byte[][].class.hashCode();
                while (-1 < --size)
                {
                    final byte[] data = src[size];
                    int len = data.length, i, k = 0;

                    for (i = 0; 3 < len; i += 4, len -= 4)
                        hash = mix(hash, data[i] & 0xFF | (data[i + 1] & 0xFF) << 8 | (data[i + 2] & 0xFF) << 16 | (data[i + 3] & 0xFF) << 24);
                    switch (len)
                    {
                        case 3:
                            k ^= (data[i + 2] & 0xFF) << 16;
                        case 2:
                            k ^= (data[i + 1] & 0xFF) << 8;
                    }

                    hash = finalizeHash(mixLast(hash, k ^ data[i] & 0xFF), data.length);
                }

                return finalizeHash(hash, src.length);
            }

            @Override
            public boolean equals(byte[][] O, byte[][] X, int len)
            {
                if (O != X)
                {
                    if (O == null || X == null || O.length < len || X.length < len)
                        return false;
                    for (byte[] o, x; -1 < --len;)
                        if ((o = O[len]) != (x = X[len]))
                            if (o == null || x == null || !Arrays.equals(o, x))
                                return false;
                }
                return true;
            }
        }

        //Singleton instance for short arrays
        public static final _shorts shorts = new _shorts();

        //Inner class for equality and hash code computation on short arrays
        public static final class _shorts extends EqualHashOf<short[]>
        {
            public static final short[] O = new short[0];

            private _shorts() { super(short[].class); }

            @Override
            public int hashCode(short[] src) { return Arrays.hashCode(src); }

            @Override
            public boolean equals(short[] v1, short[] v2) { return Arrays.equals(v1, v2); }

            @Override
            public int hashCode(short[][] src, int size)
            {
                int hash = short[][].class.hashCode();
                while (-1 < --size)
                    hash = (size + 10153331) + hash(hash, Arrays.hashCode(src[size]));
                return hash;
            }

            @Override
            public boolean equals(short[][] O, short[][] X, int len)
            {
                if (O != X)
                {
                    if (O == null || X == null || O.length < len || X.length < len)
                        return false;
                    for (short[] o, x; -1 < --len;)
                        if ((o = O[len]) != (x = X[len]))
                            if (o == null || x == null || !Arrays.equals(o, x))
                                return false;
                }
                return true;
            }
        }

        //Singleton instance for char arrays
        public static final _chars chars = new _chars();

        //Inner class for equality and hash code computation on char arrays
        public static final class _chars extends EqualHashOf<char[]>
        {
            public static final char[] O = new char[0];

            private _chars() { super(char[].class); }

            @Override
            public int hashCode(char[] src) { return Arrays.hashCode(src); }

            @Override
            public boolean equals(char[] v1, char[] v2) { return Arrays.equals(v1, v2); }

            @Override
            public int hashCode(char[][] src, int size)
            {
                int hash = char[][].class.hashCode();
                while (-1 < --size)
                    hash = (size + 10153331) + hash(hash, Arrays.hashCode(src[size]));
                return hash;
            }

            @Override
            public boolean equals(char[][] O, char[][] X, int len)
            {
                if (O != X)
                {
                    if (O == null || X == null || O.length < len || X.length < len)
                        return false;
                    for (char[] o, x; -1 < --len;)
                        if ((o = O[len]) != (x = X[len]))
                            if (o == null || x == null || !Arrays.equals(o, x))
                                return false;
                }
                return true;
            }
        }

        public static final _ints ints = new _ints();

        public static final class _ints extends EqualHashOf<int[]>
        {
            public static final int[] O = new int[0];

            private _ints() { super(int[].class); }

            @Override
            public int hashCode(int[] src) { return Arrays.hashCode(src); }

            @Override
            public boolean equals(int[] v1, int[] v2) { return Arrays.equals(v1, v2); }

            @Override
            public int hashCode(int[][] src, int size)
            {
                int hash = int[][].class.hashCode();
                while (-1 < --size)
                    hash = (size + 10153331) + hash(hash, Arrays.hashCode(src[size]));
                return hash;
            }

            @Override
            public boolean equals(int[][] O, int[][] X, int len)
            {
                if (O != X)
                {
                    if (O == null || X == null || O.length < len || X.length < len)
                        return false;
                    for (int[] o, x; -1 < --len;)
                        if ((o = O[len]) != (x = X[len]))
                            if (o == null || x == null || !Arrays.equals(o, x))
                                return false;
                }
                return true;
            }
        }

        //Inner class for long arrays
        public static final _longs longs = new _longs();

        public static final class _longs extends EqualHashOf<long[]>
        {
            public static final long[] O = new long[0];

            private _longs() { super(long[].class); }

            @Override
            public int hashCode(long[] src) { return Arrays.hashCode(src); }

            @Override
            public boolean equals(long[] v1, long[] v2) { return Arrays.equals(v1, v2); }

            @Override
            public int hashCode(long[][] src, int size)
            {
                int hash = long[][].class.hashCode();
                while (-1 < --size)
                    hash = (size + 10153331) + hash(hash, Arrays.hashCode(src[size]));
                return hash;
            }

            @Override
            public boolean equals(long[][] O, long[][] X, int len)
            {
                if (O != X)
                {
                    if (O == null || X == null || O.length < len || X.length < len)
                        return false;
                    for (long[] o, x; -1 < --len;)
                        if ((o = O[len]) != (x = X[len]))
                            if (o == null || x == null || !Arrays.equals(o, x))
                                return false;
                }
                return true;
            }
        }

        //Inner class for float arrays
        public static final _floats floats = new _floats();

        public static final class _floats extends EqualHashOf<float[]>
        {
            public static final float[] O = new float[0];

            private _floats() { super(float[].class); }

            @Override
            public int hashCode(float[] src) { return Arrays.hashCode(src); }

            @Override
            public boolean equals(float[] v1, float[] v2) { return Arrays.equals(v1, v2); }

            @Override
            public int hashCode(float[][] src, int size)
            {
                int hash = float[][].class.hashCode();
                while (-1 < --size)
                    hash = (size + 10153331) + hash(hash, Arrays.hashCode(src[size]));
                return hash;
            }

            @Override
            public boolean equals(float[][] O, float[][] X, int len)
            {
                if (O != X)
                {
                    if (O == null || X == null || O.length < len || X.length < len)
                        return false;
                    for (float[] o, x; -1 < --len;)
                        if ((o = O[len]) != (x = X[len]))
                            if (o == null || x == null || !Arrays.equals(o, x))
                                return false;
                }
                return true;
            }
        }

        //Inner class for double arrays
        public static final _doubles doubles = new _doubles();

        public static final class _doubles extends EqualHashOf<double[]>
        {
            public static final double[] O = new double[0];

            private _doubles() { super(double[].class); }

            @Override
            public int hashCode(double[] src) { return Arrays.hashCode(src); }

            @Override
            public boolean equals(double[] v1, double[] v2) { return Arrays.equals(v1, v2); }

            @Override
            public int hashCode(double[][] src, int size)
            {
                int hash = double[][].class.hashCode();
                while (-1 < --size)
                    hash = (size + 10153331) + hash(hash, Arrays.hashCode(src[size]));
                return hash;
            }

            @Override
            public boolean equals(double[][] O, double[][] X, int len)
            {
                if (O != X)
                {
                    if (O == null || X == null || O.length < len || X.length < len)
                        return false;
                    for (double[] o, x; -1 < --len;)
                        if ((o = O[len]) != (x = X[len]))
                            if (o == null || x == null || !Arrays.equals(o, x))
                                return false;
                }
                return true;
            }
        }

        //Inner interface for equality and hash code computation on objects
        public interface objects
        {
            Object[] O = new Object[0];
            Object[][] OO = new Object[0][];

            //Method to check equality between two 2D object arrays
            static boolean equals(Object[][] O, Object[][] X)
            {
                if (O != X)
                {
                    if (O == null || X == null || O.length != X.length)
                        return false;
                    for (int i = O.length; -1 < --i;)
                        if (!Arrays.equals(O[i], X[i]))
                            return false;
                }
                return true;
            }

            //Method to compute hash code of a 2D object array
            static int hash(int hash, Object[][] src)
            {
                if (src == null)
                    return hash ^ 10153331;
                for (Object[] s : src)
                    hash ^= Arrays.hashCode(s);
                return hash;
            }
        }
    }

    //Method to copy an array of type T. It returns the singleton if the result has zero length
    @SuppressWarnings("unchecked")
    static <T> T[] copyOf(T[] src, int len)
    {
        return len < 1 ? (T[])get(src.getClass()).O : Arrays.copyOf(src, len);
    }

    //Method to resize an array of type T. It returns the singleton if the result has zero length
    static <T> T[] resize(T[] src, int len, T val)
    {
        return src == null ? fill(copyOf((T[])null, len), val) : src.length == len ? src
                                                                                   : fill(copyOf(src, len), src.length, len, val);
    }

    //Method to fill an array with a specific value
    static <T> T[] fill(T[] dst, T val)
    {
        for (int i = 0, len = dst.length; i < len; i++)
            dst[i] = val;
        return dst;
    }

    //Method to fill a portion of an array with a specific value
    static <T> T[] fill(T[] dst, int fromIndex, int toIndex, T val)
    {
        for (int i = fromIndex; i < toIndex; i++)
            dst[i] = val;
        return dst;
    }

    //Method to copy a 2D array of type T
    @SuppressWarnings("unchecked")
    static <T> T[][] copyOf(T[][] src, int len)
    {
        return len < 1 ? (T[][])get(src.getClass()).OO : Arrays.copyOf(src, len);
    }

    //Method to resize a 2D array of type T
    static <T> T[][] resize(T[][] src, int len, T[] val)
    {
        return src == null ? fill(copyOf((T[][])null, len), val) : src.length == len ? src
                                                                                     : fill(copyOf(src, len), src.length, len, val);
    }

    //Method to fill a 2D array with a specific value
    static <T> T[][] fill(T[][] dst, T[] val)
    {
        for (int i = 0, len = dst.length; i < len; i++)
            dst[i] = val;
        return dst;
    }

    //Method to fill a 2D array with a specific value
    static <T> T[][] fill(T[][] dst, int fromIndex, int toIndex, T[] val)
    {
        for (int i = fromIndex; i < toIndex; i++)
            dst[i] = val;
        return dst;
    }

    //Method to copy a boolean array. It returns the singleton if the result has zero length
    static boolean[] copyOf(boolean[] src, int len)
    {
        return len < 1 ? EqualHashOf.booleans.O : Arrays.copyOf(src == null ? EqualHashOf.booleans.O : src, len);
    }

    //Method to resize a boolean array
    static boolean[] resize(boolean[] src, int len, boolean val)
    {
        return src == null ? fill(copyOf(EqualHashOf.booleans.O, len), val) : src.length == len ? src
                                                                                                : fill(copyOf(src, len), src.length, len, val);
    }

    //Method to fill a boolean array with a specific value
    static boolean[] fill(boolean[] dst, boolean val)
    {
        for (int i = 0, len = dst.length; i < len; i++)
            dst[i] = val;
        return dst;
    }

    //Method to fill a portion of a boolean array with a specific value
    static boolean[] fill(boolean[] dst, int fromIndex, int toIndex, boolean val)
    {
        for (int i = fromIndex; i < toIndex; i++)
            dst[i] = val;
        return dst;
    }

    //Method to copy a byte array. It returns the singleton if the result has zero length
    static byte[] copyOf(byte[] src, int len)
    {
        return len < 1 ? EqualHashOf.bytes.O : Arrays.copyOf(src == null ? EqualHashOf.bytes.O : src, len);
    }

    //Method to resize a byte array . It returns the singleton if the result has zero length
    static byte[] resize(byte[] src, int len, byte val)
    {
        return src == null ? fill(copyOf(EqualHashOf.bytes.O, len), val) : src.length == len ? src
                                                                                             : fill(copyOf(src, len), src.length, len, val);
    }

    //Method to fill a byte array with a specific value
    static byte[] fill(byte[] dst, byte val)
    {
        for (int i = 0, len = dst.length; i < len; i++)
            dst[i] = val;
        return dst;
    }

    //Method to fill a portion of a byte array with a specific value
    static byte[] fill(byte[] dst, int fromIndex, int toIndex, byte val)
    {
        for (int i = fromIndex; i < toIndex; i++)
            dst[i] = val;
        return dst;
    }

    //Method to copy a short array. It returns the singleton if the result has zero length
    static short[] copyOf(short[] src, int len)
    {
        return len < 1 ? EqualHashOf.shorts.O : Arrays.copyOf(src == null ? EqualHashOf.shorts.O : src, len);
    }

    //Method to resize a short array. It returns the singleton if the result has zero length
    static short[] resize(short[] src, int len, short val)
    {
        return src == null ? fill(copyOf(EqualHashOf.shorts.O, len), val) : src.length == len ? src
                                                                                              : fill(copyOf(src, len), src.length, len, val);
    }

    //Method to fill a short array with a specific value
    static short[] fill(short[] dst, short val)
    {
        for (int i = 0, len = dst.length; i < len; i++)
            dst[i] = val;
        return dst;
    }

    //Method to fill a portion of a short array with a specific value
    static short[] fill(short[] dst, int fromIndex, int toIndex, short val)
    {
        for (int i = fromIndex; i < toIndex; i++)
            dst[i] = val;
        return dst;
    }

    //Method to copy a char array. It returns the singleton if the result has zero length
    static char[] copyOf(char[] src, int len)
    {
        return len < 1 ? EqualHashOf.chars.O : Arrays.copyOf(src == null ? EqualHashOf.chars.O : src, len);
    }

    //Method to resize a char array. It returns the singleton if the result has zero length
    static char[] resize(char[] src, int len, char val)
    {
        return src == null ? fill(copyOf(EqualHashOf.chars.O, len), val) : src.length == len ? src
                                                                                             : fill(copyOf(src, len), src.length, len, val);
    }

    //Method to fill a char array with a specific value
    static char[] fill(char[] dst, char val)
    {
        for (int i = 0, len = dst.length; i < len; i++)
            dst[i] = val;
        return dst;
    }

    //Method to fill a portion of a char array with a specific value
    static char[] fill(char[] dst, int fromIndex, int toIndex, char val)
    {
        for (int i = fromIndex; i < toIndex; i++)
            dst[i] = val;
        return dst;
    }

    //Method to copy an int array. It returns the singleton if the result has zero length
    static int[] copyOf(int[] src, int len)
    {
        return len < 1 ? EqualHashOf.ints.O : Arrays.copyOf(src == null ? EqualHashOf.ints.O : src, len);
    }

    //Method to resize an int array. It returns the singleton if the result has zero length
    static int[] resize(int[] src, int len, int val)
    {
        return src == null ? fill(copyOf(EqualHashOf.ints.O, len), val) : src.length == len ? src
                                                                                            : fill(copyOf(src, len), src.length, len, val);
    }

    //Method to fill an int array with a specific value
    static int[] fill(int[] dst, int val)
    {
        for (int i = 0, len = dst.length; i < len; i++)
            dst[i] = val;
        return dst;
    }

    //Method to fill a portion of an int array with a specific value
    static int[] fill(int[] dst, int fromIndex, int toIndex, int val)
    {
        for (int i = fromIndex; i < toIndex; i++)
            dst[i] = val;
        return dst;
    }

    //Method to copy a long array. It returns the singleton if the result has zero length
    static long[] copyOf(long[] src, int len)
    {
        return len < 1 ? EqualHashOf.longs.O : Arrays.copyOf(src == null ? EqualHashOf.longs.O : src, len);
    }

    //Method to resize a long array. It returns the singleton if the result has zero length
    static long[] resize(long[] src, int len, long val)
    {
        return src == null ? fill(copyOf(EqualHashOf.longs.O, len), val) : src.length == len ? src
                                                                                             : fill(copyOf(src, len), src.length, len, val);
    }

    //Method to fill a long array with a specific value
    static long[] fill(long[] dst, long val)
    {
        for (int i = 0, len = dst.length; i < len; i++)
            dst[i] = val;
        return dst;
    }

    //Method to fill a portion of a long array with a specific value
    static long[] fill(long[] dst, int fromIndex, int toIndex, long val)
    {
        for (int i = fromIndex; i < toIndex; i++)
            dst[i] = val;
        return dst;
    }

    //Method to copy a float array. It returns the singleton if the result has zero length
    static float[] copyOf(float[] src, int len)
    {
        return len < 1 ? EqualHashOf.floats.O : Arrays.copyOf(src == null ? EqualHashOf.floats.O : src, len);
    }

    //Method to resize a float array. It returns the singleton if the result has zero length
    static float[] resize(float[] src, int len, float val)
    {
        return src == null ? fill(copyOf(EqualHashOf.floats.O, len), val) : src.length == len ? src
                                                                                              : fill(copyOf(src, len), src.length, len, val);
    }

    //Method to fill a float array with a specific value
    static float[] fill(float[] dst, float val)
    {
        for (int i = 0, len = dst.length; i < len; i++)
            dst[i] = val;
        return dst;
    }

    //Method to fill a portion of a float array with a specific value
    static float[] fill(float[] dst, int fromIndex, int toIndex, float val)
    {
        for (int i = fromIndex; i < toIndex; i++)
            dst[i] = val;
        return dst;
    }

    //Method to copy a double array. It returns the singleton if the result has zero length
    static double[] copyOf(double[] src, int len)
    {
        return len < 1 ? EqualHashOf.doubles.O : Arrays.copyOf(src == null ? EqualHashOf.doubles.O : src, len);
    }

    //Method to resize a double array
    static double[] resize(double[] src, int len, double val)
    {
        return src == null ? fill(copyOf(EqualHashOf.doubles.O, len), val) : src.length == len ? src
                                                                                               : fill(copyOf(src, len), src.length, len, val);
    }

    static double[] fill(double[] dst, double val)
    {
        for (int i = 0, len = dst.length; i < len; i++)
            dst[i] = val;
        return dst;
    }

    static double[] fill(double[] dst, int fromIndex, int toIndex, double val)
    {
        for (int i = fromIndex; i < toIndex; i++)
            dst[i] = val;
        return dst;
    }

    //Pool to store instances of EqualHashOf for different classes
    HashMap<Class<?>, Object> pool = new HashMap<>(8);

    //Method to get an instance of EqualHashOf for a specific class
    @SuppressWarnings("unchecked")
    static <T> EqualHashOf<T> get(Class<T> clazz)
    {
        final Object c = clazz;
        if (c == boolean[].class)
            return (EqualHashOf<T>)EqualHashOf.booleans;
        if (c == byte[].class)
            return (EqualHashOf<T>)EqualHashOf.bytes;
        if (c == short[].class)
            return (EqualHashOf<T>)EqualHashOf.shorts;
        if (c == int[].class)
            return (EqualHashOf<T>)EqualHashOf.ints;
        if (c == long[].class)
            return (EqualHashOf<T>)EqualHashOf.longs;
        if (c == char[].class)
            return (EqualHashOf<T>)EqualHashOf.chars;
        if (c == float[].class)
            return (EqualHashOf<T>)EqualHashOf.floats;
        if (c == double[].class)
            return (EqualHashOf<T>)EqualHashOf.doubles;

        EqualHashOf<T> ret = (EqualHashOf<T>)pool.get(clazz);
        if (ret != null)
            return ret;
        synchronized (pool)
        {
            ret = (EqualHashOf<T>)pool.get(clazz);
            if (ret != null)
                return ret;

            ret = new EqualHashOf<>(clazz);
            pool.put(clazz, ret);
            return ret;
        }
    }

    //Method to resize an array
    static int resize(Object src, Object dst, int index, int size, final int resize)
    {
        if (resize < 0)
        {
            if (index + (-resize) < size)
            {
                System.arraycopy(src, index + (-resize), dst, index, size - (index + (-resize)));
                if (src != dst)
                    System.arraycopy(src, 0, dst, 0, index);
                return size + resize;
            }
            return index;
        }
        final int new_size = Math.max(index, size) + resize;
        if (0 < size)
            if (index < size)
                if (index == 0)
                    System.arraycopy(src, 0, dst, resize, size);
                else
                {
                    if (src != dst)
                        System.arraycopy(src, 0, dst, 0, index);
                    System.arraycopy(src, index, dst, index + resize, size - index);
                }
            else if (src != dst)
                System.arraycopy(src, 0, dst, 0, size);
        return new_size;
    }

    //Method to copy elements from one array to another
    static void copy(Object src, int index, final int skip, int size, Object dst)
    {
        if (0 < index)
            System.arraycopy(src, 0, dst, 0, index);
        if ((index += skip) < size)
            System.arraycopy(src, index, dst, index, size - index);
    }

    //Method to find the next power of 2 for a given number
    static long nextPowerOf2(long v)
    {
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        v |= v >> 32;
        v++;
        return v;
    }

    //Method to check equality between two 2D object arrays
    static boolean equals(Object[][] O, Object[][] X)
    {
        //Check if the two arrays are the same reference
        if (O != X)
        {
            //If they are not the same reference, check for null values and equality of lengths
            if (O == null || X == null || O.length != X.length)
                return false;
            //Iterate over each sub-array and check for equality using Arrays.equals()
            for (int i = O.length; -1 < --i;)
                if (!Arrays.equals(O[i], X[i]))
                    return false;
        }
        return true;
    }

    //Methods for hash code computation and mixing
    static int mix(int hash, int data)
    {
        //Mix the data into the hash using bit operations
        return Integer.rotateLeft(mixLast(hash, data), 13) * 5 + 0xe6546b64;
    }

    static int mixLast(int hash, int data)
    {
        //Mix the data into the hash using bit operations
        return hash ^ Integer.rotateLeft(data * 0xcc9e2d51, 15) * 0x1b873593;
    }

    static int finalizeHash(int hash, int length)
    {
        //Finalize the hash by applying the avalanche function and XORing with the length
        return avalanche(hash ^ length);
    }

    static int avalanche(int size)
    {
        //Apply the avalanche function to the size
        size = (size ^ size >>> 16) * 0x85ebca6b;
        size = (size ^ size >>> 13) * 0xc2b2ae35;
        return size ^ size >>> 16;
    }

    //Method to compute the hash for a 2D object array
    static int hash(int hash, Object[][] src)
    {
        if (src == null)
            return 0;
        for (Object[] s : src)
            hash ^= Arrays.hashCode(s);

        return hash;
    }

    //Method to compute the hash for an object
    static int hash(int hash, Object src) { return hash ^ hash(src); }

    //Method to compute the hash for a double
    static int hash(int hash, double src) { return hash ^ hash(src); }

    //Method to compute the hash for a float
    static int hash(int hash, float src) { return hash ^ hash(src); }

    //Method to compute the hash for an integer
    static int hash(int hash, int src) { return hash ^ hash(src); }

    //Method to compute the hash for a long
    static int hash(int hash, long src) { return hash ^ hash(src); }

    //Compute the hash of an object using its hashCode()
    static int hash(Object src)
    {
        return src == null ? 0 : src.hashCode();
    }

    //Method to compute the hash for a double
    static int hash(double src) { return Double.hashCode(src); }

    //Method to compute the hash for a float
    static int hash(float src) { return Float.hashCode(src); }

    //Method to return the integer value as hash
    static int hash(int src) { return src; }

    //Method to compute the hash for a long
    static int hash(long src) { return Long.hashCode(src); }

    //Compute the hash of a string using a custom hash function
    static int hash(final String str)
    {
        if (str == null)
            return 52861;
        if (str.isEmpty())
            return 37607;

        int h = 61667;

        long x = 0;
        for (int i = 0, b = 0; i < str.length(); i++)
        {
            long ch = str.charAt(i);
            x |= ch << b;
            if ((b += ch < 0x100 ? 8 : 16) < 32)
                continue;

            h = mix(h, (int)x);
            x >>>= 32;
            b -= 32;
        }

        h = mixLast(h, (int)x);

        return finalizeHash(h, str.length());
    }

    //Method to compute the hash for a string array with a given size
    static int hash(final String[] src, int size)
    {
        int h = String[].class.hashCode();

        for (int i = src.length - 1; -1 < i; i--)
            h = mix(h, hash(src[i]));
        return finalizeHash(h, size);
    }

    //Interface for sorting
    interface ISort
    {

        //Abstract class for sorting characters with index swapping and copying
        abstract class Index implements ISort
        {
            public char[] dst;
            public int size;

            @Override
            public void swap(int ia, int ib)
            {
                final char t = dst[ia];
                dst[ia] = dst[ib];
                dst[ib] = t;
            }

            @Override
            public void copy(int idst, int isrc) { dst[idst] = dst[isrc]; }

            char fixi = 0;

            @Override
            public void drop(int idst) { dst[idst] = fixi; }
        }

        //Abstract class for sorting algorithms that operate on int arrays
        abstract class Index2 implements ISort
        {
            public int[] dst;
            public int size;

            @Override
            public void swap(int ia, int ib)
            {
                //Swap two elements in the int array
                final int t = dst[ia];
                dst[ia] = dst[ib];
                dst[ib] = t;
            }

            @Override
            public void copy(int idst, int isrc) { dst[idst] = dst[isrc]; }

            int fixi = 0;

            @Override
            public void drop(int idst) { dst[idst] = fixi; }
        }

        interface Primitives
        {
            //Interface for sorting algorithms that operate on primitive types
            long get(int index);

            default int compare(long x, long y) { return Long.compare(x, y); }

            interface floats extends Primitives
            {
                //Interface for sorting algorithms that operate on float arrays
                float get2(int index);

                default long get(int index) { return Float.floatToIntBits(get2(index)); }

                default int compare(long x, long y)
                {
                    final float X = Float.intBitsToFloat((int)x);
                    final float Y = Float.intBitsToFloat((int)y);
                    return X < Y ? -1 : Y < X ? 1
                                              : (Long.compare(x, y));
                }
            }

            interface doubles extends Primitives
            {
                //Interface for sorting algorithms that operate on double arrays
                double get2(int index);

                default long get(int index) { return Double.doubleToLongBits(get2(index)); }

                default int compare(long x, long y)
                {
                    final double X = Double.longBitsToDouble(x);
                    final double Y = Double.longBitsToDouble(y);
                    return X < Y ? -1 : Y < X ? 1
                                              : (Long.compare(x, y));
                }
            }

            class Index extends ISort.Index
            {
                //Class for sorting algorithms that operate on primitive arrays using an index
                public Primitives src;

                @Override
                public int compare(int ia, int ib) { return src.compare(src.get(dst[ia]), src.get(dst[ib])); }

                @Override
                public int compare(int isrc) { return src.compare(fix, src.get(dst[isrc])); }

                long fix = 0;

                @Override
                public void hold(int isrc) { fix = src.get(fixi = dst[isrc]); }
            }

            class Index2 extends ISort.Index2
            {
                //Class for sorting algorithms that operate on primitive arrays using an index (for int arrays)
                public Primitives src;

                @Override
                public int compare(int ia, int ib) { return src.compare(src.get(dst[ia]), src.get(dst[ib])); }

                @Override
                public int compare(int isrc) { return src.compare(fix, src.get(dst[isrc])); }

                long fix = 0;

                @Override
                public void hold(int isrc) { fix = src.get(fixi = dst[isrc]); }
            }

            class Direct implements ISort
            {
                //Class for sorting algorithms that operate directly on primitive arrays
                interface PrimitivesSet extends ISort.Primitives
                {
                    void set(int index, long value);
                }

                public PrimitivesSet array;

                @Override
                public int compare(int ia, int ib) { return array.compare(array.get(ia), array.get(ib)); }

                @Override
                public int compare(int isrc) { return array.compare(fix, array.get(isrc)); }

                long fix = 0;

                @Override
                public void hold(int isrc) { fix = array.get(fixi = isrc); }

                @Override
                public void swap(int ia, int ib)
                {
                    final long t = array.get(ia);
                    array.set(ia, array.get(ib));
                    array.set(ib, t);
                }

                @Override
                public void copy(int idst, int isrc) { array.set(idst, array.get(isrc)); }

                int fixi = 0;

                @Override
                public void drop(int idst) { array.set(idst, fixi); }
            }
        }

        //Interface for handling generic types in sorting
        interface Anything
        {

            void hold(int isrc);

            //Methods for comparing, swapping, copying, and manipulating array elements
            int compare(int ia, int ib);

            int compare(int ib);

            //Class for sorting generic types with index swapping and copying
            class Index extends ISort.Index
            {
                public Anything src;

                @Override
                public int compare(int ia, int ib) { return src.compare(dst[ia], dst[ib]); }

                @Override
                public int compare(int isrc) { return src.compare(dst[isrc]); }

                @Override
                public void hold(int isrc) { src.hold(fixi = dst[isrc]); }
            }

            //Class for sorting generic types with index swapping and copying
            class Index2 extends ISort.Index2
            {
                public Anything src;

                @Override
                public int compare(int ia, int ib) { return src.compare(dst[ia], dst[ib]); }

                @Override
                public int compare(int isrc) { return src.compare(dst[isrc]); }

                @Override
                public void hold(int isrc) { src.hold(fixi = dst[isrc]); }
            }
        }

        //Abstract class for handling objects in sorting
        abstract class Objects<T> implements Anything, Comparator<T>
        {
            abstract T get(int index);

            public int compare(T o1, T o2) { return Integer.compare(hash(o1), hash(o2)); }

            @Override
            public int compare(int ia, int ib) { return compare(get(ia), get(ib)); }

            @Override
            public int compare(int ib) { return compare(fix, get(ib)); }

            T fix;

            @Override
            public void hold(int isrc) { fix = get(isrc); }
        }

        int compare(int ia, int ib); //array[ia] < array[ib] ? -1 : array[ia] == array[ib] ? 0 : 1;

        void swap(int ia, int ib);

        void copy(int dst_index, int src_index); //replace element at dst_index with element at src_index

        int compare(int src_index); //compare fixed element with element at index;

        void hold(int src_index); //fix a value at index

        void drop(int dst_index); //put fixed value at index

        //binary search in sorted array index of the element that value is equal to the fixed element
        static int search(ISort dst, int lo, int hi)
        {
            while (lo <= hi)
            {
                final int o = lo + (hi - lo >> 1), dir = dst.compare(o);

                if (dir == 0)
                    return o;
                if (0 < dir)
                    lo = o + 1;
                else
                    hi = o - 1;
            }
            return ~lo;
        }

        //Method for binary search in a sorted array
        static void sort(ISort dst, int lo, int hi)
        {

            int len = hi - lo + 1;
            if (len < 2)
                return;

            int pow2 = 0;
            while (0 < len)
            {
                pow2++;
                len >>>= 1;
            }

            sort(dst, lo, hi, 2 * pow2);
        }

        //Method for sorting an array with a given recursion limit
        static void sort(ISort dst, final int lo, int hi, int limit)
        {

            while (hi > lo)
            {
                int size = hi - lo + 1;

                if (size < 17)
                    switch (size)
                    {
                        case 1:
                            return;
                        case 2:
                            if (dst.compare(lo, hi) > 0)
                                dst.swap(lo, hi);
                            return;
                        case 3:
                            if (dst.compare(lo, hi - 1) > 0)
                                dst.swap(lo, hi - 1);
                            if (dst.compare(lo, hi) > 0)
                                dst.swap(lo, hi);
                            if (dst.compare(hi - 1, hi) > 0)
                                dst.swap(hi - 1, hi);
                            return;
                        default:
                            for (int i = lo; i < hi; i++)
                            {
                                dst.hold(i + 1);

                                int j = i;
                                while (lo <= j && dst.compare(j) < 0)
                                    dst.copy(j + 1, j--);

                                dst.drop(j + 1);
                            }
                            return;
                    }

                if (limit == 0)
                {
                    final int w = hi - lo + 1;
                    for (int i = w >>> 1; 0 < i; i--)
                        heapify(dst, i, w, lo);

                    for (int i = w; 1 < i; i--)
                    {
                        dst.swap(lo, lo + i - 1);
                        heapify(dst, 1, i - 1, lo);
                    }
                    return;
                }

                limit--;

                final int o = lo + (hi - lo >> 1);

                if (dst.compare(lo, o) > 0)
                    dst.swap(lo, o);
                if (dst.compare(lo, hi) > 0)
                    dst.swap(lo, hi);
                if (dst.compare(o, hi) > 0)
                    dst.swap(o, hi);

                dst.hold(o);
                dst.swap(o, hi - 1);
                int l = lo, h = hi - 1;

                while (l < h)
                {
                    while (-1 < dst.compare(++l))
                        ;
                    while (dst.compare(--h) < 0)
                        ;

                    if (h <= l)
                        break;

                    dst.swap(l, h);
                }

                if (l != hi - 1)
                    dst.swap(l, hi - 1);
                sort(dst, l + 1, hi, limit);
                hi = l - 1;
            }
        }

        //Method for heapifying a portion of an array
        static void heapify(ISort dst, int i, final int w, int lo)
        {
            dst.hold(--lo + i);

            while (i <= w >>> 1)
            {
                int child = i << 1;
                if (child < w && dst.compare(lo + child, lo + child + 1) < 0)
                    child++;

                if (-1 < dst.compare(lo + child))
                    break;

                dst.copy(lo + i, lo + child);
                i = child;
            }

            dst.drop(lo + i);
        }
    }
}
