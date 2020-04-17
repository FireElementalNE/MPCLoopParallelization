class Sequential {
    public static void main(String[] args) {
        int[] A = new int[11];
        int sum = 0;
        for(int i = 1; i < 10; i++) {
            A[i - 1] = A[i] + 5;
        }
    }
}
