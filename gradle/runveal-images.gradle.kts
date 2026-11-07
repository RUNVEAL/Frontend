/*
 * Runveal 이미지(PNG 등 래스터) 공유
 *
 * design/images 안의 파일을 그대로 Android drawable-xxxhdpi 리소스로 복사한다. 변환이 필요한
 * 아이콘(design/icons, runveal-icons.gradle.kts)과 달리 복사만 하면 되므로 별도 파싱 없이
 * 단순 Copy 태스크로 끝낸다. iOS는 Assets.xcassets에서 같은 원본을 따로 가져간다.
 */

val generateRunvealImages = tasks.register<Copy>("generateRunvealImages") {
    group = "runveal"
    description = "design/images/* -> Android drawable-xxxhdpi 리소스로 복사"
    from(rootProject.layout.projectDirectory.dir("design/images"))
    into(layout.buildDirectory.dir("generated/runvealImages/res/drawable-xxxhdpi"))
}
