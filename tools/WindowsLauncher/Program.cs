using System.Diagnostics;
using System.Windows.Forms;

var launcherDirectory = AppContext.BaseDirectory;
var portableExePath = Path.Combine(launcherDirectory, "PiCalculator", "PiCalculator.exe");

if (!File.Exists(portableExePath))
{
    MessageBox.Show(
        "Не найден файл запуска portable-версии.\n\n" +
        "Ожидаемый путь:\n" + portableExePath + "\n\n" +
        "Запускайте PiCalculator-Start.exe рядом с папкой PiCalculator.",
        "PiCalculator",
        MessageBoxButtons.OK,
        MessageBoxIcon.Error
    );
    return;
}

var startInfo = new ProcessStartInfo
{
    FileName = portableExePath,
    WorkingDirectory = Path.GetDirectoryName(portableExePath)!,
    UseShellExecute = true
};

Process.Start(startInfo);
