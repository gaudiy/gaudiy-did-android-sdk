package com.example.gaudiy_did_android_sdk

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    private val TAG = "MainActivity"
    private lateinit var didButton:Button

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        didButton = view.findViewById<Button>(R.id.button_first);

        didButton.setOnClickListener {
            Toast.makeText(context, "DID認証画面", Toast.LENGTH_SHORT).show()

            // 1. middleman に リクエストを詰める
            // 2. イベントリスナーを待機(deep link)
            // 3. クエリパラメーターから did を取得
            val openURL = Intent(android.content.Intent.ACTION_VIEW)
            // TODO: IP ごとに URL を定義できるようなインターフェースにする
            openURL.data = Uri.parse("https://google.com")
            startActivity(openURL)

//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }
}