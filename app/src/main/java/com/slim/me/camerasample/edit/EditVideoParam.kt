package com.slim.me.camerasample.edit

import android.os.Parcel
import android.os.Parcelable

class EditVideoParam : Parcelable {

    public val videoPath: String

    constructor(videoPath: String) {
        this.videoPath = videoPath
    }

    constructor(parcel: Parcel){
        this.videoPath = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(this.videoPath)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EditVideoParam> {
        override fun createFromParcel(parcel: Parcel): EditVideoParam {
            return EditVideoParam(parcel)
        }

        override fun newArray(size: Int): Array<EditVideoParam?> {
            return arrayOfNulls(size)
        }
    }
}

