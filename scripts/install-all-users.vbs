' ============================================================================
' - install-all-users.vbs - Windows 所有用户安装脚本
' ----------------------------------------------------------------------------
' 该 VBScript 会为 Windows 所有用户的所有 JetBrains 产品配置 vmoptions。
'
' 注意：需要管理员权限
' ============================================================================

Set shell = CreateObject("WScript.Shell")
Set fs = CreateObject("Scripting.FileSystemObject")

' 检查管理员权限
If Not shell.Run("cmd /c net session", 0, True) = 0 Then
    MsgBox "This script requires administrator privileges." & vbCrLf & _
           "Please run as administrator.", vbCritical, "Permission Denied"
    WScript.Quit 1
End If

' 获取脚本所在目录
scriptPath = Replace(WScript.ScriptFullName, "\" & WScript.ScriptName, "")
jarPath = scriptPath & "\ja-netfilter.jar"

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

' 公共配置目录
programData = shell.ExpandEnvironmentStrings(" %%PROGRAMDATA%")
")
configBase = programData & "\JetBrains"

If Not fs.FolderExists(configBase) Then
    fs.CreateFolder(configBase)
End If

' 为每个产品创建 vmoptions
For Each strProduct In products
    vmFilePath = scriptPath & "\vmoptions\" & strProduct & ".vmoptions"
    targetFile = configBase & "\" & strProduct & ".vmoptions"

    content = ""
    If fs.FileExists(vmFilePath) Then
        Set file = fs.OpenFilePath(vmFilePath, 1)
        content = file.ReadAll
        file.Close
    End If

    Dim lines()
    lines = Split(content, vbLf)
    newContent = ""
    For Each line In lines
        If InStr(line, "ja-netfilter.jar") = 0 Then
            newContent = newContent & line & vbLf
        End If
    Next

    newContent = newContent & "-javaagent:" & jarPath & "=jetbrains" & vbLf

    Set file = fs.CreateTextFile(targetFile, True)
    file.Write newContent
    file.Close

    ' 设置系统级环境变量
    envName = UCase(strProduct) & "_VM_OPTIONS"
    shell.Environment("System").Item(envName) = targetFile
Next

MsgBox "All-users installation completed!", vbInformation, "Success"