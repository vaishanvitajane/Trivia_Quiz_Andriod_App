package com.example.quizwiz.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quizwiz.R
import com.example.quizwiz.adapter.LeaderBoardAdapter
import com.example.quizwiz.constants.Base
import com.example.quizwiz.constants.Constants
import com.example.quizwiz.databinding.ActivityLeaderBoardBinding
import com.example.quizwiz.fireBase.FireBaseClass
import com.example.quizwiz.models.LeaderBoardModel

class LeaderBoardActivity : AppCompatActivity() {
    private var binding: ActivityLeaderBoardBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderBoardBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        binding?.leaderBoardRecyclerView?.layoutManager = LinearLayoutManager(this)
        setLeaderBoard(Constants.allTimeScore)
        binding?.radioGroup?.setOnCheckedChangeListener { _, i ->
            when (i) {
                R.id.rbAllTime -> setLeaderBoard(Constants.allTimeScore)
                R.id.rbWeekly -> setLeaderBoard(Constants.weeklyScore)
                R.id.rbMonthly -> setLeaderBoard(Constants.monthlyScore)
            }
        }
    }

    private fun setLeaderBoard(type: String) {
        FireBaseClass().getLeaderBoardData(type, object : FireBaseClass.LeaderBoardDataCallback {
            override fun onLeaderBoardDataFetched(leaderBoardModel: LeaderBoardModel?) {
                if (leaderBoardModel != null) {
                    val r1 = leaderBoardModel.rank1!!
                    val r2 = leaderBoardModel.rank2!!
                    val r3 = leaderBoardModel.rank3!!

                    binding?.tvRank1Name?.text = r1.name
                    binding?.tvRank1Points?.text =
                        String.format("%.2f", Base.desiredScore(r1, type))
                    binding?.tvRank2Name?.text = r2.name
                    binding?.tvRank2Points?.text =
                        String.format("%.2f", Base.desiredScore(r2, type))
                    binding?.tvRank3Name?.text = r3.name
                    binding?.tvRank3Points?.text =
                        String.format("%.2f", Base.desiredScore(r3, type))

                    FireBaseClass().setProfileImage(r1.image,binding?.ivRank1!!)
                    FireBaseClass().setProfileImage(r2.image,binding?.ivRank2!!)
                    FireBaseClass().setProfileImage(r3.image,binding?.ivRank3!!)
                    
                    val adapter = LeaderBoardAdapter(leaderBoardModel.otherRanks, type)
                    binding?.leaderBoardRecyclerView?.adapter = adapter
                }
            }
        })
    }
}