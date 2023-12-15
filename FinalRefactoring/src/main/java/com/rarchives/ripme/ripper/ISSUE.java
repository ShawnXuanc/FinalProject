package com.rarchives.ripme.ripper;

public enum ISSUE {
    NORMAL(206), REDIRECT(3),CLIENT(4), SERVER(5), IMGURHTTP(506);
    private int num;
    private ISSUE(int i) {
        this.num = i;
    }

    public int getNum() {
        return this.num;
    }

}
