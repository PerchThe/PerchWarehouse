package dev.ev1dent.perchWarehouse.managers;

public class WarehouseManager {
    public void create(String name, int capacity) {
        System.out.println("WarehouseManager.create() " + name + " " + capacity);
    }

    public void edit(String name){
        System.out.println("WarehouseManager.edit() " + name);
    }

    public void delete(String name){
        System.out.println("WarehouseManager.delete() "+ name);
    }

    public void start(){
        System.out.println("WarehouseManager.start()");
    }

    public void stop(){
        System.out.println("WarehouseManager.stop()");
    }
}
