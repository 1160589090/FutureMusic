$(document).ready(function () {
    //音乐人切换
    $("#EM").hide();
    $("#RH").hide();
    $("#CN_tab").click(function () {
        $("#CN").show();
        $("#EM").hide();
        $("#RH").hide();
    });
    $("#EM_tab").click(function () {
        $("#EM").show();
        $("#CN").hide();
        $("#RH").hide();
    });
    $("#RH_tab").click(function () {
        $("#RH").show();
        $("#CN").hide();
        $("#EM").hide();
    });

    //排行榜动画
    $(".Ranking_container .container").hover(function () {
        $(this).hover(function () {
            let str = this.children[2].children[0];
            let yinbo = this.children[2].children[1];
            $(str).hide(300);
            $(yinbo).show(300);
        }, function () {
            let str = this.children[2].children[0];
            let yinbo = this.children[2].children[1];
            $(yinbo).hide(300);
            $(str).show(300);
        });
    });

    //轮播图箭头
    $(".LB").hover(function () {
        $(this).hover(function () {
            let con = this.children[0].children[2];
            let cons = this.children[0].children[3];
            $(con).show(300);
            $(cons).show(300);
        }, function () {
            let con = this.children[0].children[2];
            let cons = this.children[0].children[3];
            $(con).hide(300);
            $(cons).hide(300);
        })
    });
});


//头部
// $(".icon-laji").parent().click(function () {
//     console.log(111);
//     $(this).parent().parent().parent().parent().remove();
// });

var mh_a1 = document.querySelectorAll(".modal_header a")[0];
var mh_a2 = document.querySelectorAll(".modal_header a")[1];
var login = document.getElementsByClassName("login")[0];
var registered = document.getElementsByClassName("registered")[0];
$(mh_a1).onclick = function () {
    login.style.display = "block";
    registered.style.display = "none";
};
$(mh_a2).onclick = function () {
    login.style.display = "none";
    registered.style.display = "block";
};
var httpurl = "";    //请求路径

// 用于发送验证码
// $("#button4").on("click", function () {
//     var registerMail = $("#registerMail").val();
//     $.ajax({
//         type: "get",
//         url: "registerVerificationCode?mailbox=" + registerMail,
//         dataType: "json",
//         success: function (data) {
//             alert(data);
//             // alert(data.registerMail);
//             if (data.state) {
//                 // alert("我调用了");
//             }
//             $("#wc").text(data);
//             document.getElementsByClassName('reg_hed_right')[0].children[1].innerHTML = data.information;
//         }
//     });
// });

// 搜索框提示
$("#search").keyup(function () {
    var searchVal = this.value;
    console.log(searchVal);
    var search_tips = $('.search_tips')[0];
    var tips_head = "<li><a href='/musicPage?musicId='>";
    var tips_last = "</a></li>";
    var tips = "";
    if (searchVal != "") {
        $.ajax({
            contentType: "application/x-www-form-urlencoded;charset=UTF-8",
            type: "post",
            url: "/getSearchMusic",
            data: {search: searchVal},
            dataType: "json",
            success: function (data) {
                console.log(data);

                for (let i in data) {
                    var musicName = data[i].name;
                    tips_head = "<li><a href='/musicPage?condition=" + searchVal + "'>";
                    // console.log(tips_head);
                    tips = tips + tips_head + musicName + tips_last;
                }
                search_tips.innerHTML = tips;


            }
        })
    }
});

// $("#searchGo").on('click',function () {
//     var searchVal = $("#search").val();
//     console.log(searchVal)
//     // window.location.href = "/musicPage?condition=" + searchVal;
//
// });


//搜索框历史记录
$("#search").click(function () {
    var search_tips = $('.search_tips')[0];
    var tips_head = "<li><a href='./musics.html?musicId='>";
    var tips_last = "</a></li>";
    var tips = "";
    $.ajax({
        contentType: "application/x-www-form-urlencoded",
        type: "post",
        url: "searchMyRecord",
        dataType: "json",
        success: function (data) {
            alert(22);
            var history = data[1];
            console.log(history);
            for (let i in history) {
                var musicName = history[i].name;
                tips_head = "<li><a href='./musics.html?musicId=" + history[i].id + "'>";
                console.log(tips_head);
                tips = tips + tips_head + musicName + tips_last;
            }
            search_tips.innerHTML = tips;
        }
    })
});

var like = 0;
$(".thumbnail .icon-like").click(function () {
    var musicId = $(this).parent().parent().parent()[0].children[0];
    musicId = musicId.innerHTML;
    // console.log(musicId);
    // $.ajax({
    //     contentType: "application/json;charset=UTF-8",
    //     type: "post",
    //     // dataType: "json",
    //     url: "collectionMusic",
    //     data:{
    //         "musicId" : musicId,
    //         "type" : 1,
    //     },
    //     success: function (data) {
    //         console.log(111);
    //         let obj = $(this)[0];
    //         if (like === 0) {
    //             $(obj).addClass('like');
    //             like = 1;
    //         } else {
    //             $(obj).removeClass('like');
    //             like = 0;
    //         }
    //     },
    //     error:function () {
    //         console.log("错误")
    //     }
    // })
});


// //音乐人遍历
// $(".artist_container").hover(function () {
//     $.ajax({
//         contentType: "application/x-www-form-urlencoded;charset=UTF-8",
//         type: "post",
//         url: "/allSinger",
//         dataType: "json",
//         success: function (data) {
//             console.log(data);
//         }
//     })
// });
// window.onload = function (){
//     var length = $('.icon-like').length;
//     for(i = 0 ; i < length ; i++){
//         var id = $('.icon-like')[i].data('id');
//         $.ajax({
//             contentType: "application/x-www-form-urlencoded;charset=UTF-8",
//             url: "",
//             type: 'post',
//             dataType: "json",
//             success:{}
//         })
//     }
// };
// var like = 0;
// $('.icon-like').on('click', function () {
//     if (like == 0) {
//         $(this).addClass('like');
//         like = 1;
//     } else {
//         $(this).removeClass('like');
//         like = 0;
//     }
// });


$(".icon-like").on('click', function () {
    var userId = $("#userId").data('id');
    var musicId = $(this).data('id');
    console.log(musicId);
    var $thi = $(this);
    var obj = $thi[0];
    console.log(obj);
    if ($(obj).hasClass("likeMusicVideo")) {
        collectionMusic(userId, musicId, 2, $(this));
    } else if ($(obj).hasClass("likeSongList")) {
        collectionSongList(userId, musicId, 1, $(this));
    } else if ($(obj).hasClass("likeAlbum")) {
        collectionSongList(userId, musicId, 2, $(this));
    } else if ($(obj).hasClass("likeMusic")) {
        collectionMusic(userId, musicId, 1, $(this));
    }
});


function collectionMusic(userId, musicId, type, obj) {
    $.ajax({
        contentType: "application/x-www-form-urlencoded;charset=UTF-8",
        url: "/user/collectionMusic",
        type: 'post',
        dataType: "json",
        data: {
            musicId: musicId,
            type: type
        },
        success: function (data) {
            // console.log(data);
            // alert("已取消收藏！");
            // $(obj).removeClass('like');
            if (data.state == 1) {
                alert("删除收藏成功");
                obj.removeClass('like');
            } else if (data.state == 2) {
                alert("添加收藏成功");
                obj.addClass('like');
            } else {
                alert("请刷新网页")
            }

        }
    });

}

function collectionSongList(userId, musicId, type, obj) {
    $.ajax({
        contentType: "application/x-www-form-urlencoded;charset=UTF-8",
        url: "/user/collectionSongList",
        type: 'post',
        dataType: "json",
        data: {
            id: musicId,
            type: type
        },
        success: function (data) {
            // alert("已取消收藏！");
            // $(obj).removeClass('like');
            if (data.state == 1) {
                alert("删除收藏成功index");
                obj.removeClass('like');
            } else if (data.state == 2) {
                alert("添加收藏成功index");
                obj.addClass('like');
            } else {
                alert("请刷新网页")
            }

        }
    });

}

