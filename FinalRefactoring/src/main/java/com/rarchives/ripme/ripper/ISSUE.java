package com.rarchives.ripme.ripper;

public enum ISSUE {
    NORMAL(206), REDIRECT(3),CLIENT(4), SERVER(5), IMGURHTTP(503), NOTFIND(404);
    private int num;
    private ISSUE(int i) {
        this.num = i;
    }

    public int getNum() {
        return this.num;
    }

}
