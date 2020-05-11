// HPL_dgemm from HPC Challenge v1.5.0 http://icl.cs.utk.edu/hpcc/
// Lines 87 - 89
class HPL_dgemm1 {
    public static void main(String[] args) {
        int jcj = 0;
        int jal = 0;
        int[] A = new int [10];
        int[] C = new int [10];
        int M = 10;
        // t0 = ALPHA * B[iblj];
        int t0 = 1000;
        for(int i = 0, iail = jal, icij = jcj; i < M; i++, iail += 1, icij += 1) {
            C[icij] += A[iail] * t0;
        }
    }
}