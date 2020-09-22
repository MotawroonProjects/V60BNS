package com.v60BNS.activities_fragments.activity_home.fragments;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.v60BNS.R;
import com.v60BNS.activities_fragments.activity_home.HomeActivity;
import com.v60BNS.activities_fragments.activity_setting.SettingsActivity;
import com.v60BNS.adapters.Comments_Adapter;
import com.v60BNS.adapters.Post_Adapter;
import com.v60BNS.databinding.FragmentProfileBinding;
import com.v60BNS.models.NearbyStoreDataModel;
import com.v60BNS.models.PostModel;
import com.v60BNS.models.ReviewModels;
import com.v60BNS.models.StoryModel;
import com.v60BNS.models.UserModel;
import com.v60BNS.preferences.Preferences;
import com.v60BNS.remote.Api;
import com.v60BNS.share.Common;
import com.v60BNS.tags.Tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.paperdb.Paper;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Fragment_Profile extends Fragment {

    private HomeActivity activity;
    private FragmentProfileBinding binding;
    private Preferences preferences;
    private String lang;
    private UserModel userModel;
    private List<PostModel.Data> postlist;
    public BottomSheetBehavior behavior;
    private RecyclerView recViewcomments;
    private ImageView imclose, imageshare;
    private Post_Adapter post_adapter;
    private List<ReviewModels.Reviews> reviewsList;
    private Comments_Adapter comments_adapter;
    private TextView tvcount;
    private CheckBox ch_like;
    private int position;

    public static Fragment_Profile newInstance() {

        return new Fragment_Profile();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
        getprofile();
        getPosts();

    }

    private void initView() {

        postlist = new ArrayList<>();
        reviewsList = new ArrayList<>();
        activity = (HomeActivity) getActivity();
        preferences = Preferences.getInstance();
        userModel=preferences.getUserData(activity);
        Paper.init(activity);
        lang = Paper.book().read("lang", Locale.getDefault().getLanguage());

        recViewcomments = binding.getRoot().findViewById(R.id.recViewcomments);
        tvcount = binding.getRoot().findViewById(R.id.tvcount);
        imclose = binding.getRoot().findViewById(R.id.imclose);
        ch_like = binding.getRoot().findViewById(R.id.chelike);
        imageshare = binding.getRoot().findViewById(R.id.imageshare);

        post_adapter = new Post_Adapter(postlist, activity, this);

        binding.recViewFavoriteOffers.setLayoutManager(new LinearLayoutManager(activity));
        binding.recViewFavoriteOffers.setAdapter(post_adapter);
        comments_adapter = new Comments_Adapter(reviewsList, activity);
        recViewcomments.setLayoutManager(new LinearLayoutManager(activity));
        recViewcomments.setAdapter(comments_adapter);
        binding.imgSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, SettingsActivity.class);
                startActivity(intent);
            }
        });
        imageshare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                share(position);
            }
        });
        ch_like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userModel != null) {
                    like_dislike(position);
                } else {
                    recViewcomments = binding.getRoot().findViewById(R.id.recViewcomments);
                }
            }
        });
        if (lang.equals("ar")) {
            imclose.setRotation(180);
        }
        setUpBottomSheet();

    }

    private void setUpBottomSheet() {

        behavior = BottomSheetBehavior.from(binding.getRoot().findViewById(R.id.root));

    }

    public void getPosts() {

        try {
            int uid;
            if (userModel != null) {
                uid = userModel.getId();
            } else {
                uid = 0;
            }

            Api.getService(Tags.base_url).
                    getmyposts("off", uid + "").
                    enqueue(new Callback<PostModel>() {
                        @Override
                        public void onResponse(Call<PostModel> call, Response<PostModel> response) {
                            binding.progBarOffer.setVisibility(View.GONE);

                            if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {

                                postlist.clear();
                                postlist.addAll(response.body().getData());
                                if (postlist.size() > 0) {
                                    post_adapter.notifyDataSetChanged();
                                } else {

                                }

                            } else {
                                try {

                                    Log.e("error", response.code() + "_" + response.errorBody().string());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                if (response.code() == 500) {
                                    Toast.makeText(activity, "Server Error", Toast.LENGTH_SHORT).show();


                                } else {
                                    Toast.makeText(activity, getString(R.string.failed), Toast.LENGTH_SHORT).show();


                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<PostModel> call, Throwable t) {
                            binding.progBarOffer.setVisibility(View.GONE);
                            try {
                                if (t.getMessage() != null) {
                                    Log.e("error", t.getMessage());
                                    if (t.getMessage().toLowerCase().contains("failed to connect") || t.getMessage().toLowerCase().contains("unable to resolve host")) {
                                        Toast.makeText(activity, R.string.something, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(activity, t.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }

                            } catch (Exception e) {
                            }


                        }
                    });
        } catch (Exception e) {

        }


    }

    public void getPlaceDetails(String placeid, int position) {
        ch_like.setChecked(false);

        if (postlist.get(position).isLove_check()) {
            ch_like.setChecked(true);
        }
        reviewsList.clear();
        ProgressDialog dialog = Common.createProgressDialog(activity, getString(R.string.wait));
        dialog.setCancelable(false);
        this.position = position;
        // dialog.show();


        Api.getService("https://maps.googleapis.com/maps/api/")
                .getPlaceReview(placeid, getString(R.string.map_api_key))
                .enqueue(new Callback<NearbyStoreDataModel>() {
                    @Override
                    public void onResponse(Call<NearbyStoreDataModel> call, Response<NearbyStoreDataModel> response) {
                        dialog.dismiss();
                        if (response.isSuccessful() && response.body() != null && response.body().getResult().getReviews() != null) {
                            Log.e(";;;", response.body().getResult().getReviews().get(0).getAuthor_name());
                            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            Log.e("dddddata", response.body().getResult().getReviews().size() + "");

                            reviewsList.addAll(response.body().getResult().getReviews());
                            comments_adapter.notifyDataSetChanged();
                            tvcount.setText(response.body().getResult().getReviews().size() + "");
                        } else {
                            Log.e("dddddata", response.code() + "");

                        }


                    }

                    @Override
                    public void onFailure(Call<NearbyStoreDataModel> call, Throwable t) {
                        try {
                            dialog.dismiss();
                            Log.e("Error", t.getMessage());
                        } catch (Exception e) {

                        }
                    }
                });
    }

    public int like_dislike(int pos) {
        if (userModel != null) {
            try {
                Api.getService(Tags.base_url)
                        .likepost("Bearer " + userModel.getToken(), postlist.get(pos).getId() + "")
                        .enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.isSuccessful()) {

                                    getPosts();
                                } else {


                                    if (response.code() == 500) {
                                        Toast.makeText(activity, "Server Error", Toast.LENGTH_SHORT).show();


                                    } else {
                                        Toast.makeText(activity, getString(R.string.failed), Toast.LENGTH_SHORT).show();

                                        try {

                                            Log.e("error", response.code() + "_" + response.errorBody().string());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                try {

                                    if (t.getMessage() != null) {
                                        Log.e("error", t.getMessage());
                                        if (t.getMessage().toLowerCase().contains("failed to connect") || t.getMessage().toLowerCase().contains("unable to resolve host")) {
                                            Toast.makeText(activity, R.string.something, Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(activity, t.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                } catch (Exception e) {
                                }
                            }
                        });
            } catch (Exception e) {

            }
            return 1;

        } else {

            Common.CreateDialogAlert(activity, getString(R.string.please_sign_in_or_sign_up));
            return 0;

        }
    }

    public void share(int position) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, postlist.get(position).getLink_for_share());
        startActivity(intent);
    }

    public void getprofile() {
        if (userModel != null) {
            try {
                Api.getService(Tags.base_url)
                        .getprofile("Bearer " + userModel.getToken(), userModel.getPhone())
                        .enqueue(new Callback<UserModel>() {
                            @Override
                            public void onResponse(Call<UserModel> call, Response<UserModel> response) {
                                if (response.isSuccessful()) {

                                    updateprofile(response.body());
                                } else {


                                    if (response.code() == 500) {
                                        Toast.makeText(activity, "Server Error", Toast.LENGTH_SHORT).show();


                                    } else {
                                        Toast.makeText(activity, getString(R.string.failed), Toast.LENGTH_SHORT).show();

                                        try {

                                            Log.e("error", response.code() + "_" + response.errorBody().string());
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<UserModel> call, Throwable t) {
                                try {

                                    if (t.getMessage() != null) {
                                        Log.e("error", t.getMessage());
                                        if (t.getMessage().toLowerCase().contains("failed to connect") || t.getMessage().toLowerCase().contains("unable to resolve host")) {
                                            Toast.makeText(activity, R.string.something, Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(activity, t.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                } catch (Exception e) {
                                }
                            }
                        });
            } catch (Exception e) {

            }
            //return 1;

        } else {

            Common.CreateDialogAlert(activity, getString(R.string.please_sign_in_or_sign_up));

        }
    }

    private void updateprofile(UserModel body) {
        userModel = body;
        preferences.create_update_userdata(activity, userModel);
        binding.setModel(userModel);
    }

}
