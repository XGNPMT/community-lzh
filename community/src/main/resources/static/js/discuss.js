$(function (){
    $("#topBtn").click(setTop);
    $("#wonderfulBtn").click(setWonderful);
    $("#deleteBtn").click(setDelete);
})

function like (btn,entityType,entityId,entityUserId,postId){
    $.post(
        CONTEXT_PATH+"/like",
        {"entityType":entityType,"entityId":entityId,"entityUserId":entityUserId,"discussPostId":postId},
        function (data) {
            data=$.parseJSON(data);
            if(data.code==200){
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus==1?"已赞":"赞");
            }else {
                alert(data.msg);
            }
        }
    );
}
function setTop(){
    $.post(
        CONTEXT_PATH+"/discuss/top",
        {"id":$("#postId").val()},
        function (data) {
            data=$.parseJSON(data);
            if(data.code==200){
                $("#topBtn").text(data.type==1?"取消置顶":"置顶");
            }else {
                alert(data.msg);
            }
        }
    );
}
function setWonderful(){
    $.post(
        CONTEXT_PATH+"/discuss/wonderful",
        {"id":$("#postId").val()},
        function (data) {
            data=$.parseJSON(data);
            if(data.code==200){
                $("#wonderfulBtn").text(data.status==1?"取消精帖":"精帖");
            }else {
                alert(data.msg);
            }
        }
    );
}
function setDelete(){
    $.post(
        CONTEXT_PATH+"/discuss/delete",
        {"id":$("#postId").val()},
        function (data) {
            data=$.parseJSON(data);
            if(data.code==200){
                location.href=CONTEXT_PATH+"/index";
            }else {
                alert(data.msg);
            }
        }
    );
}
