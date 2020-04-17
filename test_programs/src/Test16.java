class Test16 {
    public static void main(String[] args) {
        int[] A = new int[10];
        int sum = 0;
        int sum2 = 0;
        int test = 0;
        for(int i = 0; i < 10; i++) {
            sum = sum + A[i];
            sum2 = sum2 - A[i];
            if(test % 2 == 0) {
                sum = sum + 1;
                sum2 = sum2 - 1;
            }
        }
    }
}