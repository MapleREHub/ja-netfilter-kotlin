' ============================================================================
' - install-current-user.vbs - Windows 当前用户安装脚本
' ----------------------------------------------------------------------------
' 该 VBScript 会为当前 Windows 当前用户的所有 JetBrains 产品配置 vmoptions。
'
' 使用方法：
'   双击 install-current-user.vbs
' ============================================================================

Set shell = CreateObject("WScript.Shell")
Set fs = CreateObject("Scripting.FileSystemObject")

' 获取脚本所在目录
scriptPath = Replace(WScript.ScriptFullName, "\" & WScript.ScriptName, "")
jarPath = scriptPath & "\ja-netfilter.jar"

' 检查 ja-netfilter.jar 是否存在
If Not fs.FileExists(jarPath) Then
    MsgBox "ja-netfilter.jar not found in " & scriptPath, vbCritical, "Error"
    WScript.Quit 1
End If

' JetBrains 产品列表
products = Array( _
    "idea", "clion", "phpstorm", "goland", "pycharm", _
    "webstorm", "webide", "rider", "datagrip", "rubymine", _
    "dataspell", "aqua", "rustrover", "gateway", _
    "jetbrains_client", "jetbrainsclient", "studio", "devecostudio" _
)

' 获取当前用户的配置目录
Set objShell = CreateObject("Shell.Application")
Set objFolder = objShell.Namespace(&H1A)  ' CSIDL_APPDATA
appDataPath = objFolder.Self.Path
configBase = appDataPath & "\JetBrains"

If Not fs.FolderExists(configBase) Then
    fs.CreateFolder(configBase)
End If

' 为每个产品创建 vmoptions 文件
For Each strProduct In products
    vmFilePath = scriptPath & "\vmoptions\" & strProduct & ".vmoptions"
    targetFile = configBase & "\" & strProduct & ".vmoptions"

    ' 读取原始 vmoptions
    content = ""
    If fs.FileExists(vmFilePath) Then
        Set file = fs.OpenFilePath(vmFilePath, 1)
        content = file.ReadAll
        file.Close
    End If

    ' 移除旧的 javaagent 行
    Dim lines()
    lines = Split(content, vbLf)
    newContent = ""
    For Each line In lines
        If InStr(line, "ja-netfilter.jar") = 0 Then
            newContent = newContent & line & vbLf
        End If
    Next

    ' 添加 javaagent 行
    newContent = newContent & "-javaagent:" & jarPath & "=jetbrains" & vbLf

    ' 写入目标文件
    Set file = fs.CreateTextFile(targetFile, True)
    file.Write newContent
    file.Close

    ' 设置环境变量（仅当前用户）
    envName = UCase(strProduct) & "_VM_OPTIONS"
    shell.Environment("User").Item(envName) = targetFile
Next

MsgBox "Installation completed!" & vbCrLf & vbCrLf & _
       "Activation code: code.txt or https://ckey.run", vbInformation, "Success"