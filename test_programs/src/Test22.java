class Test22 {
    public static void main(String[] args) {
        int x = 1;
        int[] A = new int[10];
        for(int i = 0; i < A.length; i++) {
            if(x % 3 == 1) {
                x = x + 1;
                A[i - x] = 20;
            } else {
                x = x - 1;
                A[i + x] = A[i] + 30;
            }
        }
    }
}