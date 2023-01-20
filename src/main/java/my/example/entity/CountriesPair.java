package my.example.entity;

import my.example.service.HashUtil;

public class CountriesPair {
    private short ccOne;
    private short ccTwo;
    private int hash;

    private CountriesPair(short ccOne, short ccTwo, int hash) {
        this.ccOne = ccOne;
        this.ccTwo = ccTwo;
        this.hash = hash;
    }

    private CountriesPair() {

    }

    public static CountriesPair create(short ccOne, short ccTwo) {

        return new CountriesPair(ccOne, ccTwo, HashUtil.genHash(ccOne, ccTwo));
    }


    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CountriesPair that = (CountriesPair) o;

        return hash == that.hash;
    }
}
