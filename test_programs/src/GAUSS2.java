
class GAUSS2 {

    private static int FIXEDPOINT_BITS = 32;
    private static int FIXEDPOINT_INTEGER_BITS = 24;
    private static int FIXEDPOINT_FRACTION_BITS = FIXEDPOINT_BITS - FIXEDPOINT_INTEGER_BITS;


    private static int fixedpt_mul_inner(int a, int b) {
        return a * b;
    }

    private static int fixedpt_mul(int a, int b)  {
        return  fixedpt_mul_inner(a,b) >> FIXEDPOINT_FRACTION_BITS;
    }

    public static void main(String[] args) {
        int[] m = new int[100];
        int[] Lm = new int [100];
        int N = Integer.parseInt(args[1]);
        int k = 100;
        int to = Integer.parseInt(args[2]);
        int i = Integer.parseInt(args[3]);
        for(int j = i; j < N; j++) {
            m[k*N+j] = m[k*N+j] - fixedpt_mul(Lm[k*N+i], m[i*N+j]);
        }
    }
}





