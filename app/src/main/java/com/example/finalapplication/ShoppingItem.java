package com.example.finalapplication;

public class ShoppingItem {
    private String id;
    private String name;
    private String familyCode;

    public ShoppingItem() {} // חובה עבור Firebase

    public ShoppingItem(String id, String name, String familyCode) {
        this.id = id;
        this.name = name;
        this.familyCode = familyCode;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getFamilyCode() { return familyCode; }
}