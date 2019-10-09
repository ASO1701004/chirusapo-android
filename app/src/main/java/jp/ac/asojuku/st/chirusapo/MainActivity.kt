package jp.ac.asojuku.st.chirusapo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
//import android.app.Activity;

import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmResults
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var realm:Realm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    override fun onResume() {
        super.onResume()
        setContentView(R.layout.activity_main)

        //インスタンスを生成
        realm = Realm.getDefaultInstance()

        //サーバーのトークンと一致する行があれば取得する
        var logintoken :String?= realm.where<account_user>()
            .equalTo("token",)
            .findAll()
        //トークンが一致すればログイン処理、しなければなにもしない
        //一致の場合トークンでログインを行いそのままホーム画面へ遷移
        //トークンがnullじゃないか確認する

        //ーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーー
        //　ここにログイン処理が入る
        //ーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーーー
        //ログイン時ユーザーの情報を追加する処理




        realm.executeTransaction {

            realm.createObject<account_user>().apply{
                //ここに保存する内容を入れる処理
                //例)name = "Rex"など

                //user_id

                //user_name

                //icon_file_name

                //token

                //ここに古い行を削除する処理をいれるかも？
            }


        }

        //ログイン時子供のアレルギー情報をサーバーと照らし合わせ、サーバーにだけ存在するものは追加、端末にしか存在しないものは削除する
        realm.executeTransaction {

            realm.createObject<account_user>().apply{
                //ここに保存する内容を入れる処理
                //例)name = "Rex"など

                //child_id

                //allergy_name

                //vaccine_name

            }


        }

    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }

//    Realm.init(this)
//    val realmConfig = RealmConfiguration.Builder()
//        .deleteRealmIfMigrationNeeded()
//        .build()
//    mRealm = Realm.getInstance(realmConfig)

}
