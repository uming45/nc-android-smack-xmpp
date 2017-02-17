package com.nhancv.hellosmack.ui.fragment;

import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.widget.ListView;
import android.widget.Toast;

import com.joanzapata.android.BaseAdapterHelper;
import com.joanzapata.android.QuickAdapter;
import com.nhancv.hellosmack.R;
import com.nhancv.hellosmack.helper.RxHelper;
import com.nhancv.hellosmack.ui.activity.ChatRoomActivity_;
import com.nhancv.xmpp.XmppPresenter;
import com.nhancv.xmpp.listener.XmppListener;
import com.nhancv.xmpp.model.BaseRoom;
import com.nhancv.xmpp.model.BaseRoster;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.ItemLongClick;
import org.androidannotations.annotations.ViewById;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jxmpp.util.XmppStringUtils;

import java.util.UUID;

/**
 * Created by Nhan Cao on 06-Sep-16.
 */
@EFragment(R.layout.fragment_groups)
public class GroupFragment extends Fragment {
    private static final String TAG = GroupFragment.class.getSimpleName();

    @ViewById(R.id.vListsItems)
    ListView vListsItems;

    QuickAdapter<BaseRoom> adapter;

    @AfterViews
    void initView() {
        adapter = new QuickAdapter<BaseRoom>(getContext(), R.layout.view_group_item) {
            @Override
            protected void convert(BaseAdapterHelper helper, BaseRoom room) {

                helper.setText(R.id.tvLastMsg, TextUtils.isEmpty(room.getLastMessage()) ? "..." : room.getLastMessage());
                helper.setVisible(R.id.tvLastMsg, !TextUtils.isEmpty(room.getLastMessage()));

                helper.setText(R.id.tvGroupId, XmppStringUtils.parseLocalpart(room.getRoomJid()));
                helper.setText(R.id.tvGroupName, room.getRoomNick() + " " + room.getMembers().size());
            }
        };
        vListsItems.setAdapter(adapter);
    }

    @ItemClick(R.id.vListsItems)
    public void listItemClick(BaseRoom baseRoom) {
        ChatRoomActivity_.intent(getContext()).roomId(baseRoom.getRoomJid()).start();
    }

    @ItemLongClick(R.id.vListsItems)
    public void listItemLongClick(BaseRoom baseRoom) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Leave group");
        builder.setMessage("Are you sure to leave this group?");
        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            XmppPresenter.getInstance().leaveRoom(baseRoom.getMultiUserChat());
            updateAdapter();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
        });
        builder.show();
    }

    @Click(R.id.btCreateRoom)
    public void btCreateRoomOnClick() {
        RxHelper.aSyncTask(subscriber -> {
            try {
                XmppPresenter.getInstance().createGroupChat(
                        "Test group chat",
                        "Test group description",
                        UUID.randomUUID().toString(),
                        XmppPresenter.getInstance().getCurrentUser(),
                        new XmppListener.CreateGroupListener() {
                            @Override
                            public void joined(MultiUserChat chatRoom) {
                                showToast("User already joined the room");
                            }

                            @Override
                            public void created(MultiUserChat chatRoom) {
                                showToast("Successfully created a new room");
                                updateAdapter();

                                for (BaseRoster user : XmppPresenter.getInstance().getCurrentRosterList()) {
                                    try {
                                        chatRoom.invite(user.getJid(), "hi you");
                                    } catch (SmackException.NotConnectedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            @Override
                            public void exists(MultiUserChat chatRoom) {
                                showToast("The same room is already exists");
                            }
                        }, participantPresence -> {
                            updateAdapter();
                            showToast("processPresence: " + participantPresence.getJid() + " " + participantPresence.getRole());
                        });
            } catch (XMPPException.XMPPErrorException | SmackException e) {
                e.printStackTrace();
            }
        });
    }

    public void updateAdapter() {
        RxHelper.runOnUi(() -> {
            if (adapter != null)
                adapter.replaceAll(XmppPresenter.getInstance().getRoomList());
        });
    }

    public void showToast(String msg) {
        RxHelper.runOnUi(() -> {
            Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
        });
    }

}
