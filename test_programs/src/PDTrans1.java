// pdtrans from HPC Challenge v1.5.0 http://icl.cs.utk.edu/hpcc/
// Lines 158 - 159
class PDTrans1 {
    public static void main(String[] args) {
        int it = 1;
        int jj = 2;
        int t_dim1 = 3;
        int ia = 4;
        int ja = 5;
        int jt = 6;
        int a_dim1 = 7;
        int i__3 = 8;
        int[] t = new int [10];
        int[] a = new int [10];
        for(int k = 1; k <= i__3; ++k) {
            t[it + jj + (jt + k) * t_dim1] += a[ia + k + (ja + jj) * a_dim1];
            /* L100: */
        }
    }
}