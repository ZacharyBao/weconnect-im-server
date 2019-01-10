package SocketServer.dto;

import java.util.List;

/**
 * Created by BaoDong on 2017/11/30.
 */
public class Group {
    private String groupId;
    private List<String> groupUserIdList;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public List<String> getGroupUserIdList() {
        return groupUserIdList;
    }

    public void setGroupUserIdList(List<String> groupUserIdList) {
        this.groupUserIdList = groupUserIdList;
    }
}
