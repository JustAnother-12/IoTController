package com.example.iotcontroller;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iotcontroller.model.IoTDevice;
import com.example.iotcontroller.services.SensorService;

import java.util.ArrayList;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private Context context;
    private ArrayList<IoTDevice> IoTDevices;

    public DeviceAdapter(Context context, ArrayList<IoTDevice> IoTDevices){
        this.context = context;
        this.IoTDevices = IoTDevices;
    }
    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textName;
        private TextView textIP;
        private ToggleButton connectToggle;

        public ViewHolder(@NonNull View itemView){
            super(itemView);
            textName = itemView.findViewById(R.id.item_name);
            textIP = itemView.findViewById(R.id.item_IP);
            connectToggle = itemView.findViewById(R.id.item_tgl);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View deviceView = inflater.inflate(R.layout.recycler_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(deviceView);

        viewHolder.connectToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                int position = viewHolder.getBindingAdapterPosition();
                IoTDevice device = IoTDevices.get(position);

                if(isChecked){
                    Intent intent = new Intent(parent.getContext(), SensorService.class);
                    intent.setAction("ACTION_PAIR_DEVICE");
                    intent.putExtra("IP_ADDRESS", device.getIP());
                    parent.getContext().startService(intent);
                }else{
                    Intent intent = new Intent(parent.getContext(), SensorService.class);
                    intent.setAction("ACTION_UNPAIR_DEVICE");
                    parent.getContext().startService(intent);
                }
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IoTDevice device = IoTDevices.get(position);
        holder.textName.setText(device.getName());
        holder.textIP.setText(device.getIP());
    }

    @Override
    public int getItemCount() {
        return IoTDevices != null ? IoTDevices.size() : 0;
    }

    public void updateData(ArrayList<IoTDevice> devices){
        IoTDevices.clear();
        IoTDevices.addAll(new ArrayList<>(devices));
        notifyDataSetChanged();
    }
}
