package us.shandian.flashbak.helper.contact;

public class ContactInfo
{
	public String name = "";
	public int rawId = -1;
	public String email = "";
	public String number = "";
	
	public void setRawId(int rawId) {
		this.rawId = rawId;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public void setNumber(String number) {
		this.number = number;
	}
	
	public int getRawId() {
		return rawId;
	}
	
	public String getName() {
		return name;
	}
	
	public String getEmail() {
		return email;
	}
	
	public String getNumber() {
		return number;
	}
}
