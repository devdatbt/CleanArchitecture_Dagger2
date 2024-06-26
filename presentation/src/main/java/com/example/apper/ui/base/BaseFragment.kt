package com.example.apper.ui.base

import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.apper.ui.MainActivity

open class BaseFragment(@LayoutRes layoutId: Int) : Fragment(layoutId) {
    protected val appComponent by lazy { (requireActivity() as MainActivity).appComponent }
}