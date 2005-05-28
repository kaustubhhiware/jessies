#!/usr/bin/ruby -w

require 'cgi'

def outputChanges(changes)
  body = ""
  changes.each {
    |line|
    color = "black"
    if line =~ /^Modified: /
      next
    elsif line =~ /^@@ /
      color = "gray"
    elsif line =~ /^-/
      color = "red"
    elsif line =~ /^\+/
      color = "blue"
    end
    body << "<font color=\"#{color}\">"
    line = CGI.escapeHTML(line)
    # Use <tt> for ---/+++ so they line up, and use distinctive background
    # colors.
    line.gsub!(/^(---|\+\+\+)(.*)$/) {
      # We can't use "---" and "+++" as class names, sadly.
      class_name = ($1 == "---" ? "triple-minus-line" : "triple-plus-line")
      |prefix| "<div class=\"#{class_name}\"><tt>#{prefix.gsub(' ', '&nbsp;')}</tt>#$2</div>"
    }
    # Write the per-line prefix characters in fixed width
    line.gsub!(/^([-+ ])/) {
      |prefix| "<tt>#{prefix.gsub(' ', '&nbsp;')}</tt>"
    }
    # Don't collapse indentation
    line.gsub!(/( {2,})/) {
      |s| s.gsub(' ', '&nbsp;')
    }
    line.gsub!(/\t/, '&nbsp;&nbsp;&nbsp;&nbsp;')
    body << "#{line}<br></font>\n"
  }
  return body
end

def outputFormattedChanges(asciiArt, changes)
  body = ""
  if asciiArt
    body << "<tt>"
    body << outputChanges(changes)
    body << "</tt>"
  else
    body << outputChanges(changes)
  end
  return body
end

def patchToHtmlEmail(from_address, to_address, reply_to_address, subject, preamble, changes)
  body = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n"
  body << "<html>\n"
  body << "<head>\n"
  body << " <title>#{subject}</title>\n"
  
  body << "  <style>\n"
  body << ".revision-header {background: #cccccc; padding-top: 6pt; padding-bottom: 6pt;}\n"
  body << ".check-in-comment {background: #eeeeee; padding-bottom: 6pt;}\n"
  body << ".triple-minus-line {background: #ffcccc;}\n"
  body << ".triple-plus-line {background: #ccccff;}\n"
  body << "  </style>\n"
  
  body << " </head>\n"
  body << "<body>\n"
  body << preamble
  
  # Determine if we should output it in a fixed width font
  asciiArt = false
  changesSoFar = Array.new
  
  changes.each {
    |line|
    if line =~ /^=====/
      body << outputFormattedChanges(asciiArt, changesSoFar)
      asciiArt = false
      changesSoFar = Array.new
      next
    end
    
    # ETextArea says 3 spaces and you're art
    if line =~ /^.\s*\S.*\s{3,}/
      asciiArt = true
    end
    changesSoFar.push(line)
  }
  body << outputFormattedChanges(asciiArt, changesSoFar)

  body << "</body></html>"
  
  # Write the header.
  header = ""
  header << "To: #{to_address}\n"
  header << "From: #{from_address}\n"
  header << "Subject: #{subject}\n"
  header << "Reply-to: #{reply_to_address}\n" if reply_to_address != nil
  header << "MIME-Version: 1.0\n"
  header << "Content-Type: text/html; charset=UTF-8\n"
  header << "Content-Transfer-Encoding: 8bit\n"
  header << "\n"

  sendmail="/usr/sbin/sendmail"

  # Send the mail.
  begin
    fd = open("|#{sendmail} #{to_address}", "w")
    fd.print(header)
    fd.print(body)
  rescue
    exit(1)
  end
  fd.close
end
