package com.ngbj.browser2.event;

public class UpdateDataEvent {
    private int index;
    public UpdateDataEvent(int index ){
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
