// HPL_logsort from HPC Challenge v1.5.0 http://icl.cs.utk.edu/hpcc/
// Lines 155 - 168
class HPL_logsort1 {
    public static void main(String[] args) {
        int k = 1;
        int j = 1;
        int iplen_i;
        int itmp;
        int NPROCS = 10;
        int iplen_j = 10;
        int[] IPMAPM1 = new int [10];
        int[] IPLEN = new int [10];
        int[] IPMAP = new int [10];
        for( int i = 2; i < NPROCS; i++ ) {
            if( k < IPMAPM1[i] ) {
                iplen_i = IPLEN[i+1]; iplen_j = IPLEN[j+1];
                if( iplen_j < iplen_i ) {
                    IPLEN[j+1] = iplen_i;  IPLEN[i+1] = iplen_j;
                    itmp       = IPMAP[j]; IPMAP[j]   = IPMAP[i];
                    IPMAP[i]   = itmp;
                }
            }
        }
    }
}


