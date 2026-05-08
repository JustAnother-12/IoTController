package com.example.iotcontroller;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iotcontroller.model.IoTDevice;
import com.example.iotcontroller.model.IoTDeviceRepository;
import com.example.iotcontroller.services.SensorService;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private Context context;
    private ArrayList<IoTDevice> IoTDevices;
    private IoTDeviceRepository ioTDeviceRepository;
    private LifecycleOwner lifecycleOwner;

    public DeviceAdapter(Context context, ArrayList<IoTDevice> IoTDevices, IoTDeviceRepository ioTDeviceRepository, LifecycleOwner owner){
        this.context = context;
        this.IoTDevices = IoTDevices;
        this.ioTDeviceRepository = ioTDeviceRepository;
        this.lifecycleOwner = owner;
    }
    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textName;
        private TextView textIP;
        private MaterialButton btnConnect;

        public ViewHolder(@NonNull View itemView){
            super(itemView);
            textName = itemView.findViewById(R.id.item_name);
            textIP = itemView.findViewById(R.id.item_IP);
            btnConnect = itemView.findViewById(R.id.item_btn);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View deviceView = inflater.inflate(R.layout.recycler_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(deviceView);

        viewHolder.btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = viewHolder.getBindingAdapterPosition();
                IoTDevice device = IoTDevices.get(position);

                if(viewHolder.btnConnect.getText().equals("Connect")){
                    Intent intent = new Intent(parent.getContext(), SensorService.class);
                    intent.setAction("ACTION_PAIR_DEVICE");
                    intent.putExtra("IP_ADDRESS", device.getIP());
                    parent.getContext().startService(intent);
                }else if(viewHolder.btnConnect.getText().equals("Disconnect")){
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

        ioTDeviceRepository.getPairedDevice().observe(lifecycleOwner, pairedDevice -> {
            if(pairedDevice != null){
                if(pairedDevice.getIP().equals(device.getIP()))
                    holder.btnConnect.setText("Disconnect");
            }else{
                holder.btnConnect.setText("Connect");
            }
        });
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
