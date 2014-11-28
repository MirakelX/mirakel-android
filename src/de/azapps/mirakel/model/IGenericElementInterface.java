package de.azapps.mirakel.model;

import android.os.Parcelable;

public interface IGenericElementInterface extends Parcelable {

    public String getName();

    public void save();

    public void destroy();
}
