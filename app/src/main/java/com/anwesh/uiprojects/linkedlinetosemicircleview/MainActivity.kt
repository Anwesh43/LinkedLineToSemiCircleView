package com.anwesh.uiprojects.linkedlinetosemicircleview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.linetosemicircleview.LineToSemiCircleView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LineToSemiCircleView.create(this)
    }
}
