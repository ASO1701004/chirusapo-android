package jp.ac.asojuku.st.chirusapo

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import android.os.Bundle
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import jp.ac.asojuku.st.chirusapo.apis.Api
import jp.ac.asojuku.st.chirusapo.apis.ApiError
import jp.ac.asojuku.st.chirusapo.apis.ApiParam
import jp.ac.asojuku.st.chirusapo.apis.ApiPostTask
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    //グローバル変数としてRealmの変数を作る
    lateinit var realm:Realm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //onCreateメソッドでRealmのインスタンスを取得する
    }

    //Realmのインスタンスを解放
    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

    // グループ作成
    private fun groupCreate(){

        val inputView = View.inflate(this, R.layout.layout_group_create, null)
        realm = Realm.getDefaultInstance()
        //関連付け
        val layoutGroupId = inputView.findViewById(R.id.group_id) as TextInputLayout
        val layoutGroupName = inputView.findViewById(R.id.group_name) as TextInputLayout

        layoutGroupId.editText?.addTextChangedListener(object:TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                val inputGroupId = layoutGroupId.editText?.text.toString().trim()

                if (inputGroupId.length < 5) {
                    layoutGroupId.error = "5文字以上で入力してください"
                } else if (inputGroupId.length > 30) {
                    layoutGroupId.error = "30文字以下で入力してください"
                } else if (!Pattern.compile("^[a-zA-Z0-9-_]{1,30}\$").matcher(inputGroupId).find()) {
                    layoutGroupId.error = "使用できない文字が含まれています"
                } else {
                    layoutGroupId.error = null
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
        })

        layoutGroupName.editText?.addTextChangedListener(object :TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                val inputGroupName = layoutGroupName.editText?.text.toString().trim()

                if (inputGroupName.isEmpty()) {
                    layoutGroupName.error = "1文字以上で入力してください"
                } else if (inputGroupName.length > 30) {
                    layoutGroupName.error = "30文字以下で入力してください"
                } else if (!Pattern.compile("^.{1,30}\$").matcher(inputGroupName).find()) {
                    layoutGroupName.error = "使用できない文字が含まれています"
                } else {
                    layoutGroupName.error = null
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
        })

        //Dialog生成
        AlertDialog.Builder(this)
            .setTitle("グループ作成")
            .setView(inputView)
            .setPositiveButton(
                "作成"
            ) { dialog, which ->
                val token_group = "sTFhvUCcVLQqAkxQN60pfCaHyU7Dg2"
                val groupId = layoutGroupId.editText?.text.toString()
                val groupName = layoutGroupName.editText?.text.toString()

                //グループ参加・作成でのRealmの保存と送信するためトークンを取得
                //トークンの取得(ApiPostTaskに送るデータだからそれより上に書いて)
                var account = realm.where<Account>().findFirst()
//                var token = account?.Rtoken

                // APIとの通信を行う
                ApiPostTask{
                    // 処理した結果が帰ってくる
                    if (it == null) {
                        ApiError.showToast(this, ApiError.CONNECTION_ERROR, Toast.LENGTH_SHORT)
                    }
                    //なにかしら返答があった場合
                    else {
                        //statusを取得する
                        when (it.getString("status")) {
                            "200" -> {
                                var num1 :Int = 1
                                var num2 :Int = 0
                                realm.executeTransaction{
                                    var group = realm.where<JoinGroup>().equalTo("Rgroup_flag",num1).findAll()
                                    if(group != null){
                                        for(x in group){
                                            x.Rgroup_flag = num2
                                        }
                                    }
                                }
                                //参加・作成したグループ情報の取得
                                val belongGroup = it.getJSONObject("data").getJSONArray("belong_group")
                                realm.executeTransaction{
                                    for (i in 0 until belongGroup.length()) {
                                        val groupInfo = belongGroup.getJSONObject(i)
                                        val groupInfoGroupId = groupInfo.getString("group_id")
                                        val groupInfoGroupName = groupInfo.getString("group_name")
                                        if(realm.where<JoinGroup>().equalTo("Rgroup_id",groupInfoGroupId).findFirst() == null){

                                                realm.createObject<JoinGroup>().apply{
                                                    Rgroup_id = groupInfoGroupId
                                                    Rgroup_name = groupInfoGroupName
                                                    //現在見ているグループに設定するためフラグを(1)にする
                                                    Rgroup_flag = num1
                                                }
                                        }
                                    }
                                    //realmに保存する
                                }
                            }
                            "400" -> {
                                val errorArray = it.getJSONArray("message")
                                for (i in 0 until errorArray.length()) {
                                    when (errorArray.getString(i)) {
                                        ApiError.VALIDATION_GROUP_ID -> {
                                            // 指定した値が正しくない
                                            ApiError.showToast(
                                                this,
                                                errorArray.getString(i),
                                                Toast.LENGTH_SHORT
                                            )
                                        }
                                        ApiError.VALIDATION_GROUP_NAME -> {
                                            // 指定して値が正しくない
                                            ApiError.showToast(
                                                this,
                                                errorArray.getString(i),
                                                Toast.LENGTH_SHORT
                                            )
                                        }
                                        ApiError.UNKNOWN_TOKEN -> {
                                            // ログイントークンの検証失敗
                                            val intent = Intent(this, SignInActivity::class.java).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            startActivity(intent)
                                        }
                                        ApiError.ALREADY_CREATE_GROUP -> {
                                            // 既に同じグループIDが登録している場合
                                            ApiError.showToast(
                                                this,
                                                errorArray.getString(i),
                                                Toast.LENGTH_SHORT
                                            )

                                        }
                                        ApiError.REQUIRED_PARAM -> {
                                            // 必要な値が見つかりませんでした表示
                                            ApiError.showToast(
                                                this,
                                                errorArray.getString(i),
                                                Toast.LENGTH_SHORT
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.execute(ApiParam(
                    Api.SLIM + "group/create" ,
                    hashMapOf("token" to token_group,"group_id" to groupId,"group_name" to groupName)
                ))
            }
            .setNegativeButton("キャンセル", null)
            .create()
            .show()
    }

    // グループ参加
    private fun groupJoin(){
        val inputView = View.inflate(this, R.layout.layout_group_join, null)

        // 関連付け
        val layoutGroupId = inputView.findViewById(R.id.group_id) as TextInputLayout
        val layoutGroupPin = inputView.findViewById(R.id.pin_code) as TextInputLayout

        layoutGroupId.editText?.addTextChangedListener(object :TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
                val inputGroupId = layoutGroupId.editText?.text.toString().trim()

                if(inputGroupId.length < 5){
                    layoutGroupId.error = "5文字以上入力してください"
                }else if (inputGroupId.length > 30) {
                    layoutGroupId.error = "30文字以下で入力してください"
                } else if (!Pattern.compile("^[a-zA-Z0-9-_]{1,30}\$").matcher(inputGroupId).find()) {
                    layoutGroupId.error = "使用できない文字が含まれています"
                } else {
                    layoutGroupId.error = null
                }
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
        })

        layoutGroupPin.editText?.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(p0: Editable?) {
               val inputGroupPin = layoutGroupPin.editText?.text.toString().trim()

                if(!Pattern.compile("^[0-9]{4}$").matcher(inputGroupPin).find()){
                    layoutGroupPin.error = "4文字の数字で入力してください"
                }else{
                        layoutGroupPin.error = null
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
        })


        // Dialog生成
        AlertDialog.Builder(this)
            .setTitle("グループ参加")
            .setView(inputView)
            .setPositiveButton(
                "参加"
            ) { _, _ ->
                val token_group = "sTFhvUCcVLQqAkxQN60pfCaHyU7Dg2"
                val groupId = layoutGroupId.editText?.text.toString()
                val groupPin = layoutGroupPin.editText?.text.toString()

                // APIとの通信を行う
                ApiPostTask{
                    // 処理した結果が帰ってくる
                    if (it == null) {
                        ApiError.showToast(this, ApiError.CONNECTION_ERROR, Toast.LENGTH_SHORT)
                    }
                    //なにかしら返答があった場合
                    else {
                        //statusを取得する
                        when (it.getString("status")) {
                            "200" -> {
                                var num1 :Int = 1
                                var num2 :Int = 0
                                realm.executeTransaction{
                                    var group = realm.where<JoinGroup>().equalTo("Rgroup_flag",num1).findAll()
                                    if(group != null){
                                        for(x in group){
                                            x.Rgroup_flag = num2
                                        }
                                    }
                                }
                                //参加・作成したグループ情報の取得
                                val belongGroup = it.getJSONObject("data").getJSONArray("belong_group")
                                realm.executeTransaction{
                                    for (i in 0 until belongGroup.length()) {
                                        val groupInfo = belongGroup.getJSONObject(i)
                                        val groupInfoGroupId = groupInfo.getString("group_id")
                                        val groupInfoGroupName = groupInfo.getString("group_name")
                                        if(realm.where<JoinGroup>().equalTo("Rgroup_id",groupInfoGroupId).findFirst() == null){

                                            realm.createObject<JoinGroup>().apply{
                                                Rgroup_id = groupInfoGroupId
                                                Rgroup_name = groupInfoGroupName
                                                //現在見ているグループに設定するためフラグを(1)にする
                                                Rgroup_flag = num1
                                            }
                                        }
                                    }
                                    //realmに保存する
                                }
                            }
                            "400" -> {
                                val errorArray = it.getJSONArray("message")
                                for (i in 0 until errorArray.length()) {
                                    when (errorArray.getString(i)) {
                                        ApiError.VALIDATION_GROUP_ID -> {
                                            // 指定した値が正しくない
                                            ApiError.showToast(
                                                this,
                                                errorArray.getString(i),
                                                Toast.LENGTH_SHORT
                                            )
                                        }
                                        ApiError.UNKNOWN_TOKEN -> {
                                            // ログイントークンの検証失敗
                                            val intent = Intent(this, SignInActivity::class.java).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            startActivity(intent)
                                        }
                                        ApiError.VALIDATION_PIN_CODE -> {
                                            // 指定した値が正しくない
                                            ApiError.showToast(
                                                this,
                                                errorArray.getString(i),
                                                Toast.LENGTH_SHORT
                                            )
                                        }
                                        ApiError.UNKNOWN_GROUP -> {
                                            // グループが見つかりませんでした表示
                                            ApiError.showToast(
                                                this,
                                                errorArray.getString(i),
                                                Toast.LENGTH_SHORT
                                            )
                                        }
                                        ApiError.VERIFY_PIN_CODE -> {
                                            // PINコードの検証に失敗しました表示
                                            ApiError.showToast(
                                                this,
                                                errorArray.getString(i),
                                                Toast.LENGTH_SHORT
                                            )
                                        }
                                        ApiError.ALREADY_BELONG_GROUP -> {
                                            // 既にグループに所属しています表示
                                            ApiError.showToast(
                                                this,
                                                errorArray.getString(i),
                                                Toast.LENGTH_SHORT
                                            )
                                        }
                                        ApiError.REQUIRED_PARAM -> {
                                            // 必要な値が見つかりませんでした表示
                                            ApiError.showToast(
                                                this,
                                                errorArray.getString(i),
                                                Toast.LENGTH_SHORT
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }.execute(ApiParam(
                    Api.SLIM + "group/join" ,
                    hashMapOf("token" to token_group,"group_id" to groupId,"pin_code" to groupPin)
                ))
            }
            .setNegativeButton("キャンセル", null)
            .create()
            .show()
    }
}
