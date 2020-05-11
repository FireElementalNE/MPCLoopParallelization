// HPL_dtrsv1 from HPC Challenge v1.5.0 http://icl.cs.utk.edu/hpcc/
// Lines 277 - 278
class HPL_dtrsv1 {
    public static void main(String[] args) {
        int jaj = 0;
        int INCX = 1;
        int j = 10;
        int[] A = new int [10];
        int[] X = new int [10];
        int t0 = 1000;
        for (int i = 0, iaij = jaj, ix = 0;
        i < j ;
        i++, iaij += 1, ix += INCX ) {
            t0 -= A[iaij] * X[ix];
        }
    }
}