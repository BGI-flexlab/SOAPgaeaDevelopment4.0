package org.bgi.flexlab.gaea.data.mapreduce.writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;

public class WindowsBasedWritable extends
		WindowsBasedBasicWritable {
	private IntWritable position = new IntWritable();

	@Override
	public void set(String winInfo, int pos) {
		this.windowsInfo.set(winInfo);
		this.position.set(pos);
	}

	public String toString() {
		return windowsInfo.toString() + "\t" + position.get();
	}

	public String getChromosomeName() {
		String[] win = windowsInfo.toString().split(":");
		return win[win.length - 2];
	}

	public Text getWindows() {
		return windowsInfo;
	}

	public String getWindowsInformation() {
		return windowsInfo.toString();
	}

	public int getWindowsNumber() {
		String[] win = windowsInfo.toString().split(":");
		return Integer.parseInt(win[win.length - 1]);
	}

	public IntWritable getPosition() {
		return position;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		windowsInfo.readFields(in);
		position.readFields(in);
	}

	public void write(DataOutput out) throws IOException {
		windowsInfo.write(out);
		position.write(out);
	}

	@Override
	public int hashCode() {
		return windowsInfo.hashCode() * 163 + position.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof WindowsBasedWritable) {
			WindowsBasedWritable tmp = (WindowsBasedWritable) other;
			return windowsInfo.toString().equals(tmp.getWindowsInformation())
					&& position.get() == (tmp.getPosition().get());
		}
		return false;
	}

	@Override
	public int compareTo(WindowsBasedBasicWritable tp) {
		int cmp = windowsInfo.compareTo(tp.getWindows());
		if (cmp != 0) {
			return cmp;
		}
		WindowsBasedWritable tmp = (WindowsBasedWritable)tp;
		return this.position.compareTo(tmp.position);
	}
}
