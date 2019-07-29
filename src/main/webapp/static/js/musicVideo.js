var url = window.location.search;
var musicVideoId = url.substring(url.lastIndexOf('musicVideoId=') + 13, url.length);
console.log('ID=' + musicVideoId);

var information = $(".music_video")[0].children[0].children[0]

var obj_id = $(".ILike h2")[0];
musicVideoId = $(obj_id).data("id");
console.log(musicVideoId);
playMusicVideo(musicVideoId);

$.ajax({
    contentType: "application/x-www-form-urlencoded;charset=UTF-8",
    url: "musicVideoInformation",
    type: 'post',
    dataType: "json",
    data: {musicVideoId: musicVideoId},
    success: function (data) {
        console.log("ok");
        console.log(data);
    }
});


// $(".ILike .ILikeMusicVideo").click(function () {
//     var mv = $(this).children()[0];
//     console.log(mv);
// });

function playMusicVideo(musicVideoId) {

    $.ajax({
        contentType: "application/x-www-form-urlencoded;charset=UTF-8",
        url: "playMusicVideo",
        type: 'post',
        dataType: "json",
        data: {id: musicVideoId},
        success: function (data) {
            if (data.id == 0) {
                information.innerHTML = "对不起，我们还未获得这支MV的版权";
            } else if (data.id == 1) {
                information.innerHTML = "对不起，这支MV需要VIP授权播放";
            } else if (data.id == 2) {
                information.innerHTML = "对不起，您还没有购买这支MV";
            } else {
                information.innerHTML = data.name;
                var video = $("video");
                $(video).attr('src', data.path);
            }
        }
    });
}

var type = 2;
var like = 0;
$('.icon-like').on('click', function () {
    let obj = $(this)[0];
    if (like === 0) {
        $(obj).addClass('like');
        $.ajax({
            contentType: "application/x-www-form-urlencoded;charset=UTF-8",
            url: "collectionMusic",
            type: 'post',
            dataType: "json",
            data: {
                musicId: musicVideoId,
                type: type
            },
            success: function (data) {
                console.log(data);
            }
        });
        like = 1;
    } else {
        $(obj).removeClass('like');
        like = 0;
    }
    // 点击小红心


});

window.onload = function () {
    // 点击发送邮箱按钮触发的事件
    $(".xin").on("click", function () {
        var id=$("#userId").data("id");
        var music=$(this).data("id");
        $.ajax({
            contentType: "application/x-www-form-urlencoded;charset=UTF-8",
            url: "/user/collectionMusic",
            type: 'post',
            dataType: "json",
            data: {
                userId: id,
                musicId: music,
                type: 2
            },
            success: function (data) {
                if(data.state==1){
                    alert("删除收藏成功")
                }else if(data.state==2){
                    alert("添加收藏成功")
                }else{
                    alert("请刷新网页")
                }
            }
        });
    })
};