class Test23 {
    public static void main(String[] args) {
        int[] A = new int[10];
        for(int i = 0; i < 10; i++) {
            int x = A[i] * 2;
            if(x > i) {
                A[i - 1] = 20;
                break;
            } else {
                A[i + 1] = A[i] + 30;
            }
        }
    }
}