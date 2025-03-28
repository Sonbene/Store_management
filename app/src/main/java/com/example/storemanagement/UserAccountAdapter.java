package com.example.storemanagement;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import java.util.List;

public class UserAccountAdapter extends ArrayAdapter<UserAccount> {
    public UserAccountAdapter(Context context, List<UserAccount> accounts) {
        super(context, 0, accounts);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_user_account, parent, false);
        }
        final UserAccount account = getItem(position);
        TextView tvUserName = convertView.findViewById(R.id.tvUserName);
        Button btnEdit = convertView.findViewById(R.id.btnEdit);
        Button btnDelete = convertView.findViewById(R.id.btnDelete);

        tvUserName.setText(account.getUserName());

        btnEdit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // Gọi dialog chỉnh sửa từ Activity thông qua callback
                if(getContext() instanceof UserManagementActivity){
                    ((UserManagementActivity)getContext()).showAccountDialog(account);
                }
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // Xác nhận xóa
                new AlertDialog.Builder(getContext())
                        .setTitle("Xóa tài khoản")
                        .setMessage("Bạn có chắc muốn xóa tài khoản này?")
                        .setPositiveButton("Xóa", new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(getContext() instanceof UserManagementActivity){
                                    ((UserManagementActivity)getContext()).deleteAccount(account.getUserName());
                                }
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });

        return convertView;
    }
}
