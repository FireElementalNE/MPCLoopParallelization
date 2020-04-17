class BothSeqVec {
    public static void main(String[] args) {
        int[] A = new int[11];
        int[] B = new int[11];
        int sum = 0;
        for(int i = 1; i < 11; i++) {
            sum = sum + B[i];
            A[i - 1] = A[i] + 5;
        }
    }
}
